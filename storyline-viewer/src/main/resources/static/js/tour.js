/*
 * The tour engine.
 *
 * One page, one iframe. The iframe holds the live JobRunr Pro dashboard and never gets modified;
 * everything the tour does happens in the parent document: a beacon pinned to a real dashboard
 * element, a popup that narrates it, and a pill that always knows the way back.
 *
 * Same-origin is what makes this possible. We read the dashboard's DOM to find anchors, click its
 * own navigation links for instant page changes, and watch its location to notice when a visitor
 * wanders off. Add ?tourDebug=1 to see every anchor decision in the console.
 */
(function () {
    'use strict';

    const DATA = JSON.parse(document.getElementById('tour-data').textContent);
    const ROOT = document.getElementById('tour');

    const SECURITY_ENABLED = ROOT.dataset.securityEnabled === 'true';
    const USER_EMAIL = ROOT.dataset.user || '';
    const LIVE_DEMO = ROOT.dataset.liveDemo === 'true';
    const DEBUG = new URLSearchParams(location.search).has('tourDebug');

    const ANCHOR_TIMEOUT = 6000;   // the dashboard has to fetch and render before an anchor exists
    const REVEAL_TIMEOUT = 2000;   // after we opened a collapsed panel for the visitor
    const FALLBACK_TIMEOUT = 2000;
    const ANCHOR_POLL = 120;
    const BEACON_TICK = 150;       // the MUI tree mutates constantly, polling beats observing
    const OFF_TRACK_TICK = 300;
    const AUTH_POLL = 3000;
    const POPUP_SETTLE = 450;      // show the narration centered if the anchor is still resolving
    const POPUP_NUDGE = 24;        // move the popup once its beacon drifted this far
    const PILL_LANE = 92;          // bottom strip the pill owns, no popup may sit in it

    const GATED = { anonymous: false };   // anything that writes to the bank needs an email first

    const STEPS = new Map(DATA.steps.map(step => [step.number, step]));
    const FIRST_STEP = DATA.steps[0].number;
    const LAST_STEP = DATA.steps[DATA.steps.length - 1].number;

    const $ = id => document.getElementById(id);
    const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
    const escapeHtml = text => String(text == null ? '' : text).replace(/[&<>"']/g,
        c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const fileNameOf = codeReference => codeReference.split('/').pop().split('?')[0];
    const debug = (...args) => { if (DEBUG) console.info('[tour]', ...args); };

    function track(event, detail) {
        if (window.clarity) window.clarity('event', event);
        if (window.gtag) window.gtag('event', event, detail || {});
        debug(event, detail || '');
    }

    /* ------------------------------------------------------------------ progress */

    const Progress = (function () {
        const KEY = 'jobrunr-tour-progress';
        let state = { completed: [], lastStep: FIRST_STEP, introSeen: false };
        try {
            Object.assign(state, JSON.parse(localStorage.getItem(KEY)) || {});
        } catch (e) {
            debug('no readable progress, starting fresh');
        }

        function persist() {
            try {
                localStorage.setItem(KEY, JSON.stringify(state));
            } catch (e) {
                debug('progress not persisted');
            }
        }

        return {
            get completed() { return state.completed; },
            get lastStep() { return state.lastStep; },
            get introSeen() { return state.introSeen; },
            has(number) { return state.completed.includes(number); },
            visit(number) {
                state.lastStep = number;
                if (!state.completed.includes(number)) state.completed.push(number);
                persist();
            },
            introDone() { state.introSeen = true; persist(); },
            reset() { state = { completed: [], lastStep: FIRST_STEP, introSeen: false }; persist(); }
        };
    })();

    /* ------------------------------------------------------------------ dashboard preferences */

    /**
     * The Pro dashboard hides the Labels column by default and remembers that choice per job state in
     * localStorage, which the tour shares with the iframe because both live on this origin. Step 9 is
     * entirely about labels, so switch the column on before anyone gets there. Any other column the
     * visitor picked survives; only labels is forced.
     */
    const DashboardColumns = {
        STATES: ['awaiting', 'scheduled', 'enqueued', 'processing', 'processed', 'succeeded', 'failed', 'deleted'],

        // What the dashboard itself starts from. Written out because a model that omits a column makes
        // it visible, so a partial write would turn on every debug column at once.
        DEFAULTS: {
            id: true, labels: true, jobName: true, clusterName: true, jobInStateSince: true,
            jobSignature: false, exceptionType: false, exceptionMessage: false, reason: false,
            updatedAt: false, createdAt: false, processDuration: false, latency: false, deleteAt: false,
            progressBar: false, parentJob: false, awaitingOn: false, rateLimiter: false, priority: false,
            dynamicQueue: false, server: false
        },

        showLabels() {
            this.STATES.forEach(state => {
                const key = state + '-jobs-column-visibility-model';
                let chosen = null;
                try { chosen = JSON.parse(localStorage.getItem(key)); } catch (e) { debug('unreadable ' + key); }
                try {
                    localStorage.setItem(key, JSON.stringify(Object.assign({}, chosen || this.DEFAULTS, { labels: true })));
                } catch (e) {
                    debug('could not preset the labels column');
                }
            });
        }
    };

    /* ------------------------------------------------------------------ stage */

    const Stage = {
        frame: $('tour-stage'),

        get win() { try { return this.frame.contentWindow; } catch (e) { return null; } },
        get doc() { try { return this.frame.contentDocument; } catch (e) { return null; } },

        path() {
            const win = this.win;
            try { return win && win.location ? win.location.pathname : null; } catch (e) { return null; }
        },

        here() {
            const win = this.win;
            try { return win && win.location ? win.location.pathname + win.location.search : null; } catch (e) { return null; }
        },

        /** The first load is in flight while the first step opens; nothing to decide until it lands. */
        async ready() {
            const win = this.win;
            const href = win && win.location ? win.location.href : '';
            if (href && href !== 'about:blank') return;
            await this.loaded();
        },

        /**
         * Moves the dashboard to a page, preferring the soft route and reloading only when the target
         * lives outside the dashboard SPA. Never reassigns src after the first load.
         */
        async navigate(url) {
            await this.ready();
            if (sameLocation(this.here(), url)) return;

            if (this.softNavigate(url)) {
                debug('soft nav to', url);
                await wait(120);
                return;
            }

            const win = this.win;
            debug('reloading stage at', url);
            if (win) win.location.replace(url); else this.frame.src = url;
            await this.loaded();
        },

        /**
         * Re-routes the SPA without leaving a trace in the browser's history. Clicking the dashboard's
         * own links would be simpler, but a react-router link pushes state, and history entries made
         * inside an iframe join the parent's session history: the back button would then walk the
         * visitor through every dashboard page the tour ever opened instead of leaving the tour.
         * Replacing the URL and announcing it as a popstate gets the same render for free.
         */
        softNavigate(url) {
            const win = this.win;
            const here = this.path();
            if (!win || !here) return false;

            const root = '/' + here.split('/')[1];
            if (url !== root && !url.startsWith(root + '/')) return false;

            win.history.replaceState(win.history.state, '', url);
            win.dispatchEvent(new win.PopStateEvent('popstate', { state: win.history.state }));
            return true;
        },

        loaded(timeout = ANCHOR_TIMEOUT) {
            return new Promise(resolve => {
                const done = () => { clearTimeout(timer); this.frame.removeEventListener('load', done); resolve(); };
                const timer = setTimeout(done, timeout);
                this.frame.addEventListener('load', done);
            });
        },

        /**
         * A node only counts once the visitor could actually see it. Being in the DOM is not enough:
         * MUI leaves the contents of a collapsed panel fully laid out inside a zero-height clip, so a
         * bare querySelector would happily anchor the beacon to thin air.
         */
        visible(selector) {
            const doc = this.doc;
            if (!doc || !selector) return null;
            let node;
            try { node = doc.querySelector(selector); } catch (e) { return null; }
            if (!node) return null;

            const rect = node.getBoundingClientRect();
            if (rect.width <= 2 || rect.height <= 2) return null;
            if (node.offsetParent === null) return null;

            for (let parent = node.parentElement; parent; parent = parent.parentElement) {
                const clipped = parent.clientHeight === 0 || parent.clientWidth === 0;
                if (clipped && doc.defaultView.getComputedStyle(parent).overflow !== 'visible') return null;
            }
            return node;
        },

        waitFor(selector, timeout) {
            if (!selector) return Promise.resolve(null);
            const deadline = Date.now() + timeout;
            return new Promise(resolve => {
                const attempt = () => {
                    const node = this.visible(selector);
                    if (node || Date.now() > deadline) return resolve(node);
                    setTimeout(attempt, ANCHOR_POLL);
                };
                attempt();
            });
        }
    };

    const wait = ms => new Promise(resolve => setTimeout(resolve, ms));

    function normalizeLocation(url) {
        const parsed = new URL(url, location.origin);
        const params = Array.from(parsed.searchParams.entries()).sort();
        return parsed.pathname + '?' + params.map(([k, v]) => k + '=' + v).join('&');
    }

    function sameLocation(a, b) {
        return Boolean(a) && Boolean(b) && normalizeLocation(a) === normalizeLocation(b);
    }

    /**
     * Selector, then the panel that hides it, then the fallback, then nothing. Every step of the
     * chain is logged under ?tourDebug=1 so a JobRunr Pro upgrade can be swept in one pass.
     */
    async function resolveAnchor(anchor) {
        if (!anchor) return null;

        let node = await Stage.waitFor(anchor.selector, ANCHOR_TIMEOUT);
        if (node) {
            debug('anchor', anchor.selector, 'resolved');
            return node;
        }

        if (anchor.reveal) {
            const reveal = Stage.visible(anchor.reveal);
            if (reveal) {
                debug('anchor', anchor.selector, 'hidden, opening', anchor.reveal);
                reveal.click();
                node = await Stage.waitFor(anchor.selector, REVEAL_TIMEOUT);
                if (node) return node;
            }
        }

        node = await Stage.waitFor(anchor.fallbackSelector, FALLBACK_TIMEOUT);
        debug('anchor', anchor.selector, node ? 'fell back to ' + anchor.fallbackSelector : 'unresolved');
        return node;
    }

    /* ------------------------------------------------------------------ beacon */

    const Beacon = {
        node: $('tour-beacon'),
        target: null,
        point: null,
        box: null,
        timer: null,
        onMove: null,

        attach(target) {
            this.target = target;
            this.scrollIntoView();
            this.tick();
            if (!this.timer) this.timer = setInterval(() => this.tick(), BEACON_TICK);
        },

        detach() {
            this.target = null;
            this.point = null;
            this.box = null;
            this.node.hidden = true;
            if (this.timer) { clearInterval(this.timer); this.timer = null; }
        },

        scrollIntoView() {
            const frame = Stage.frame.getBoundingClientRect();
            const rect = this.target.getBoundingClientRect();
            if (rect.top < 0 || rect.bottom > frame.height) {
                this.target.scrollIntoView({ block: 'center', behavior: 'smooth' });
            }
        },

        tick() {
            if (!this.target || document.hidden) return;

            const point = this.locate();
            this.node.hidden = !point;
            if (!point) { this.point = null; this.box = null; return; }

            this.box = this.measure(point);

            this.node.style.left = point.x + 'px';
            this.node.style.top = point.y + 'px';

            const moved = !this.point || Math.hypot(point.x - this.point.x, point.y - this.point.y) > POPUP_NUDGE;
            this.point = point;
            if (moved && this.onMove) this.onMove();
        },

        /**
         * Small controls get a beacon in the middle, wide containers get one just inside the top
         * left corner: a dot floating in the centre of a jobs table points at nothing in particular.
         */
        locate() {
            const rect = this.target.getBoundingClientRect();
            if (!rect.width || !rect.height) return null;

            const compact = rect.width < 220 && rect.height < 120;
            const frame = Stage.frame.getBoundingClientRect();
            const x = frame.left + rect.left + (compact ? rect.width / 2 : 18);
            const y = frame.top + rect.top + (compact ? rect.height / 2 : Math.min(rect.height / 2, 20));

            const inside = x > frame.left + 4 && x < frame.right - 4 && y > frame.top + 4 && y < frame.bottom - 4;
            return inside ? { x, y } : null;
        },

        /**
         * The rectangle the popup should step around. Anchors that take up most of the screen (a jobs
         * table) get shrunk back to the beacon itself: there is no "beside" a full-width element.
         */
        measure(point) {
            const rect = this.target.getBoundingClientRect();
            const frame = Stage.frame.getBoundingClientRect();
            const wide = rect.width > window.innerWidth * 0.45;
            const tall = rect.height > window.innerHeight * 0.45;
            return {
                left: wide ? point.x - 14 : frame.left + rect.left,
                right: wide ? point.x + 14 : frame.left + rect.right,
                top: tall ? point.y - 14 : frame.top + rect.top,
                bottom: tall ? point.y + 14 : frame.top + rect.bottom
            };
        },

        flash() {
            this.node.classList.add('is-flashing');
            setTimeout(() => this.node.classList.remove('is-flashing'), 2400);
        }
    };

    /* ------------------------------------------------------------------ toast */

    const Toast = {
        node: $('tour-toast'),
        timer: null,

        show(message, isError) {
            this.node.textContent = message;
            this.node.classList.toggle('tour-toast--error', Boolean(isError));
            this.node.hidden = false;
            clearTimeout(this.timer);
            this.timer = setTimeout(() => { this.node.hidden = true; }, 4000);
        }
    };

    /* ------------------------------------------------------------------ side panels */

    const AppPanel = {
        node: $('tour-app-panel'),
        frame: $('tour-app-frame'),
        title: $('tour-app-title'),

        open(url, title) {
            this.frame.src = url;
            this.title.textContent = title;
            this.node.hidden = false;
        },

        close() {
            if (this.node.hidden) return;
            this.node.hidden = true;
            this.frame.src = 'about:blank';
        }
    };

    const CodePanel = {
        node: $('tour-code-panel'),
        files: $('tour-code-files'),
        source: $('tour-code-source'),

        open(references, index) {
            this.node.hidden = false;
            this.files.replaceChildren(...references.map((reference, i) => {
                const tab = document.createElement('button');
                tab.type = 'button';
                tab.className = 'tour-chip tour-code__file' + (i === index ? ' is-active' : '');
                tab.textContent = fileNameOf(reference);
                tab.addEventListener('click', () => this.open(references, i));
                return tab;
            }));
            this.load(references[index]);
        },

        async load(reference) {
            this.source.textContent = 'Loading ' + fileNameOf(reference) + '…';
            this.source.removeAttribute('data-highlighted');
            try {
                const response = await fetch('/code/' + reference, { credentials: 'same-origin' });
                const markup = await response.text();
                const code = new DOMParser().parseFromString(markup, 'text/html').querySelector('code');
                this.source.textContent = code ? code.textContent : markup;
            } catch (e) {
                this.source.textContent = 'Could not load ' + fileNameOf(reference) + '.';
            }
            this.source.removeAttribute('data-highlighted');
            if (window.hljs) window.hljs.highlightElement(this.source);
        },

        close() { this.node.hidden = true; }
    };

    /* ------------------------------------------------------------------ authentication */

    const Auth = {
        signedIn: ROOT.dataset.authenticated === 'true',
        poller: null,

        get required() { return SECURITY_ENABLED; },

        blocks(action) { return this.required && !this.signedIn && !action.anonymous; },

        async refresh() {
            try {
                const response = await fetch('/tour/me', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
                if (!response.ok) return this.signedIn;
                const identity = await response.json();
                this.signedIn = Boolean(identity.authenticated);
            } catch (e) {
                debug('could not read /tour/me');
            }
            return this.signedIn;
        },

        /** The magic link opens in another tab; this one simply keeps asking until it is let in. */
        watch(onSignedIn) {
            this.stopWatching();
            this.poller = setInterval(async () => {
                if (document.hidden) return;
                if (await this.refresh()) { this.stopWatching(); onSignedIn(); }
            }, AUTH_POLL);
        },

        stopWatching() {
            if (this.poller) { clearInterval(this.poller); this.poller = null; }
        },

        /** Tells the sign-in success handler which step to come back to. */
        rememberReturn() {
            const target = '/tour/step/' + Tour.current;
            document.cookie = 'tour-return=' + target + '; path=/; max-age=1800; samesite=Lax';
        }
    };

    /* ------------------------------------------------------------------ popup */

    const Popup = {
        node: $('tour-popup'),
        step: null,
        placement: 'auto',
        minimized: false,

        prepare(step) {
            this.step = step;
            this.placement = step.placement;
            this.minimized = false;
            this.node.hidden = true;
            this.node.replaceChildren(...this.build(step));
            Pill.showResume(false);
        },

        /** Rebuilds the current step's popup in place, keeping it where it already sits. */
        prepareAndShow() {
            const point = Beacon.point;
            this.prepare(this.step);
            this.show(point);
        },

        build(step) {
            const act = DATA.acts[step.act];
            const parts = [arrow(), head(step, act), title(step), narration(step)];

            if (step.screenshot) parts.push(screenshot(step));
            if (step.liveNotice && LIVE_DEMO) parts.push(notice(step.liveNotice));

            const actions = document.createElement('div');
            actions.className = 'tour-popup__actions';
            actions.id = 'tour-popup-actions';
            step.actions.forEach(action => actions.appendChild(actionButton(step, action)));
            if (step.mode === 'app' && step.tryItUrl) actions.appendChild(appButton(step));
            parts.push(actions);

            const feedback = document.createElement('div');
            feedback.id = 'tour-popup-feedback';
            parts.push(feedback);

            const extras = extraChips(step);
            if (extras) parts.push(extras);
            parts.push(navigation(step));
            return parts;
        },

        show(point) {
            this.minimized = false;
            Pill.showResume(false);
            this.node.hidden = false;
            this.node.className = 'tour-popup';

            if (this.step.mode === 'info' || !point) {
                this.center(this.step.mode === 'info');
                return;
            }
            this.placeNextTo(point);
        },

        center(wide) {
            const node = this.node;
            node.classList.toggle('tour-popup--centered', Boolean(wide));
            node.style.left = Math.round((window.innerWidth - node.offsetWidth) / 2) + 'px';
            node.style.top = Math.round((usableHeight() - node.offsetHeight) / 2) + 'px';
        },

        placeNextTo(point) {
            const node = this.node;
            const gap = 18;
            const margin = 14;
            const width = node.offsetWidth;
            const height = node.offsetHeight;
            const viewport = { width: window.innerWidth, height: usableHeight() };
            const box = Beacon.box || { left: point.x, right: point.x, top: point.y, bottom: point.y };

            const room = {
                right: viewport.width - box.right,
                left: box.left,
                bottom: viewport.height - box.bottom,
                top: box.top
            };
            const order = this.placement === 'auto'
                ? ['right', 'left', 'bottom', 'top']
                : [this.placement, 'right', 'left', 'bottom', 'top'];
            const needed = side => (side === 'right' || side === 'left' ? width : height) + gap + margin;
            const side = order.find(candidate => room[candidate] >= needed(candidate));

            if (!side) { this.center(false); return; }

            node.classList.add('tour-popup--' + side);
            let left, top;
            if (side === 'right' || side === 'left') {
                left = side === 'right' ? box.right + gap : box.left - gap - width;
                top = clamp(point.y - 40, margin, viewport.height - height - margin);
            } else {
                top = side === 'bottom' ? box.bottom + gap : box.top - gap - height;
                left = clamp(point.x - 48, margin, viewport.width - width - margin);
            }
            node.style.left = Math.round(left) + 'px';
            node.style.top = Math.round(top) + 'px';
            this.pointArrow(side, point, { left, top, width, height });
        },

        pointArrow(side, point, box) {
            const arrowNode = this.node.querySelector('.tour-popup__arrow');
            if (!arrowNode) return;
            if (side === 'right' || side === 'left') {
                arrowNode.style.left = '';
                arrowNode.style.top = clamp(point.y - box.top - 6, 14, box.height - 26) + 'px';
            } else {
                arrowNode.style.top = '';
                arrowNode.style.left = clamp(point.x - box.left - 6, 14, box.width - 26) + 'px';
            }
        },

        /** Content or its anchor moved, so the card has to find its footing again. */
        reflow() {
            if (this.node.hidden || this.minimized || !this.step) return;
            this.node.className = 'tour-popup';
            if (this.step.mode === 'info' || !Beacon.point) { this.center(this.step.mode === 'info'); return; }
            this.placeNextTo(Beacon.point);
        },

        minimize() {
            if (this.node.hidden) return;
            this.node.hidden = true;
            this.minimized = true;
            Pill.showResume(true);
        },

        restore() {
            if (!this.minimized) return;
            this.show(Beacon.point);
        },

        feedback(message, kind) {
            const slot = $('tour-popup-feedback');
            if (!slot) return;
            if (!message) { slot.replaceChildren(); this.reflow(); return; }
            const box = document.createElement('div');
            box.className = 'tour-feedback' + (kind ? ' tour-feedback--' + kind : '');
            box.textContent = message;
            slot.replaceChildren(box);
            this.reflow();
        },

        /** The last thing the tour does is ask, which is the one thing the old storyline never did. */
        showFinale() {
            // Borrows the last step's identity so reflow and the pill keep working on the closing card.
            this.step = Object.assign({}, STEPS.get(LAST_STEP), { mode: 'info' });
            this.node.replaceChildren(...finale());
            this.minimized = false;
            this.node.hidden = false;
            this.node.className = 'tour-popup';
            this.center(true);
            Pill.showResume(false);
        },

        askForTrial() {
            const slot = $('tour-popup-actions');
            if (!slot) return;
            this.feedback(null);
            slot.replaceChildren(trialForm());
            this.reflow();
            track('tour-trial-prompt', {});
        },

        askToSignIn() {
            const slot = $('tour-popup-actions');
            if (!slot) return;
            this.feedback(null);
            Auth.rememberReturn();
            slot.replaceChildren(signInForm());
            this.reflow();
            track('tour-login-prompt', { step: this.step.number });
        }
    };

    /** Everything but the strip the pill lives in, so the popup can never hide its own controls. */
    function usableHeight() {
        return window.innerHeight - PILL_LANE;
    }

    function arrow() {
        const node = document.createElement('span');
        node.className = 'tour-popup__arrow';
        return node;
    }

    function head(step, act) {
        const node = document.createElement('div');
        node.className = 'tour-popup__head';
        node.innerHTML =
            '<span class="tour-popup__act">' + act.icon + ' ' + escapeHtml(act.name) + '</span>' +
            '<span class="tour-popup__count">Step ' + step.number + ' of ' + DATA.totalSteps + '</span>';

        const minimize = document.createElement('button');
        minimize.type = 'button';
        minimize.className = 'tour-popup__minimize';
        minimize.title = 'Hide this while you explore';
        minimize.setAttribute('aria-label', 'Hide this while you explore');
        minimize.innerHTML = '<i class="fas fa-minus"></i>';
        minimize.addEventListener('click', () => Popup.minimize());
        node.appendChild(minimize);
        return node;
    }

    function title(step) {
        const node = document.createElement('h2');
        node.className = 'tour-popup__title';
        node.textContent = step.title;
        return node;
    }

    function narration(step) {
        const node = document.createElement('div');
        node.className = 'tour-popup__narration';
        node.innerHTML = step.narration;
        return node;
    }

    function screenshot(step) {
        const node = document.createElement('img');
        node.className = 'tour-popup__shot';
        node.src = step.screenshot;
        node.alt = step.title;
        // The card is placed before the image has a height, so it has to settle again once it does.
        node.addEventListener('load', () => Popup.reflow());
        return node;
    }

    function notice(message) {
        const node = document.createElement('p');
        node.className = 'tour-popup__notice';
        node.textContent = message;
        return node;
    }

    /**
     * Every trigger looks the same and behaves the same when it is locked: it asks for an email right
     * here rather than handing the visitor to /login, which is where the old guide lost most of them.
     */
    function triggerButton(icon, label, locked, run) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'tour-action' + (locked ? ' tour-action--locked' : '');
        button.innerHTML = '<i class="' + icon + '"></i><span>' + escapeHtml(label) + '</span>' +
            (locked ? '<i class="fas fa-lock tour-action__lock"></i>' : '');
        button.addEventListener('click', () => locked ? Popup.askToSignIn() : run(button));
        return button;
    }

    function actionButton(step, action) {
        return triggerButton('fas fa-bolt', action.label, Auth.blocks(action),
            button => runAction(step, action, button));
    }

    /** The banking forms all write, so the panel is gated exactly like the one-click triggers are. */
    function appButton(step) {
        return triggerButton('fas fa-building-columns', 'Open JobRunr Finance', Auth.blocks(GATED), () => {
            AppPanel.open(step.tryItUrl, 'JobRunr Finance');
            track('tour-app-opened', { step: step.number });
        });
    }

    function extraChips(step) {
        const chips = [];

        step.codeReferences.forEach((reference, index) => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'tour-chip';
            chip.innerHTML = '<i class="fas fa-code"></i><span>' + escapeHtml(fileNameOf(reference)) + '</span>';
            chip.addEventListener('click', () => {
                CodePanel.open(step.codeReferences, index);
                track('tour-code-opened', { step: step.number });
            });
            chips.push(chip);
        });

        if (step.externalUrl && !LIVE_DEMO) {
            const link = document.createElement('a');
            link.className = 'tour-chip';
            link.href = step.externalUrl;
            link.target = '_blank';
            link.rel = 'noopener';
            link.innerHTML = '<i class="fas fa-arrow-up-right-from-square"></i><span>Open it</span>';
            chips.push(link);
        }

        if (step.learnMore) {
            const link = document.createElement('a');
            link.className = 'tour-chip';
            link.href = step.learnMore;
            link.target = '_blank';
            link.rel = 'noopener';
            link.innerHTML = '<i class="fas fa-book"></i><span>Docs</span>';
            chips.push(link);
        }

        if (!chips.length) return null;
        const row = document.createElement('div');
        row.className = 'tour-popup__extras';
        chips.forEach(chip => row.appendChild(chip));
        return row;
    }

    function navigation(step) {
        const row = document.createElement('div');
        row.className = 'tour-popup__nav';

        const previous = document.createElement('button');
        previous.type = 'button';
        previous.className = 'tour-btn tour-btn--ghost';
        previous.innerHTML = '<i class="fas fa-arrow-left"></i><span>Back</span>';
        previous.disabled = step.number === FIRST_STEP;
        previous.addEventListener('click', () => Tour.open(step.number - 1));
        row.appendChild(previous);

        const next = document.createElement('button');
        next.type = 'button';
        next.className = 'tour-btn tour-btn--primary tour-btn--next';
        const last = step.number === LAST_STEP;
        next.innerHTML = last
            ? '<span>Finish</span><i class="fas fa-trophy"></i>'
            : '<span>Next</span><i class="fas fa-arrow-right"></i>';
        next.addEventListener('click', () => last ? Tour.finish() : Tour.open(step.number + 1));
        row.appendChild(next);
        return row;
    }

    function finale() {
        const head = document.createElement('div');
        head.className = 'tour-popup__head';
        head.innerHTML = '<span class="tour-popup__act">Closing time</span>' +
            '<span class="tour-popup__count">' + DATA.totalSteps + ' of ' + DATA.totalSteps + '</span>';

        const title = document.createElement('h2');
        title.className = 'tour-popup__title';
        title.textContent = 'You just watched a bank run. The good kind.';

        const body = document.createElement('div');
        body.className = 'tour-popup__narration';
        body.textContent = 'Retries, batches, mutexes, priority queues, rate limiters, server tags, external jobs '
            + 'and an audit trail, all on the dashboard you have been poking at since step 1. Every pattern '
            + 'you saw is production code.';

        // Asking is the whole point of the closing card, so the form opens here instead of on jobrunr.io.
        const actions = document.createElement('div');
        actions.className = 'tour-popup__actions';
        actions.id = 'tour-popup-actions';
        const ask = document.createElement('button');
        ask.type = 'button';
        ask.className = 'tour-action';
        ask.innerHTML = '<i class="fas fa-rocket"></i><span>Request a free JobRunr Pro trial</span>';
        ask.addEventListener('click', () => Popup.askForTrial());
        actions.appendChild(ask);
        actions.appendChild(linkAction('fas fa-book-open', 'Read the written guide, with all the code', '/storyline', true));

        const feedback = document.createElement('div');
        feedback.id = 'tour-popup-feedback';

        const restart = document.createElement('div');
        restart.className = 'tour-popup__nav';
        const again = document.createElement('button');
        again.type = 'button';
        again.className = 'tour-link';
        again.textContent = 'Take the tour again';
        again.addEventListener('click', () => { Progress.reset(); Tour.open(FIRST_STEP); });
        restart.appendChild(again);

        return [head, title, body, actions, feedback, restart];
    }

    function trialForm() {
        const form = document.createElement('form');
        form.className = 'tour-auth';
        form.innerHTML =
            '<p class="tour-auth__lead">A JobRunr Pro trial license, and someone from the team to answer ' +
            'questions about your setup. No call to book, we promise.</p>' +
            '<input class="tour-auth__field" type="email" name="email" placeholder="you@company.com" required ' +
            'autocomplete="email" value="' + escapeHtml(USER_EMAIL) + '">' +
            '<input class="tour-auth__field" type="text" name="company" placeholder="Company" autocomplete="organization">' +
            '<div class="tour-auth__row">' +
            '<button class="tour-btn tour-btn--primary" type="submit">Request the trial</button>' +
            '<button class="tour-link" type="button" data-cancel>Not now</button>' +
            '</div>';

        form.querySelector('[data-cancel]').addEventListener('click', () => Popup.showFinale());
        form.addEventListener('submit', event => {
            event.preventDefault();
            submitTrial(form);
        });
        return form;
    }

    async function submitTrial(form) {
        const submit = form.querySelector('button[type="submit"]');
        const email = form.elements.email.value.trim();
        submit.disabled = true;
        Popup.feedback(null);

        const utm = new URLSearchParams(location.search);
        let ok = false;
        try {
            const response = await fetch('/tour/trial', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({
                    email: email,
                    company: form.elements.company.value.trim(),
                    utm_source: utm.get('utm_source') || '',
                    utm_medium: utm.get('utm_medium') || '',
                    utm_campaign: utm.get('utm_campaign') || '',
                    utm_term: utm.get('utm_term') || '',
                    utm_content: utm.get('utm_content') || ''
                })
            });
            ok = response.ok;
        } catch (e) {
            ok = false;
        }

        submit.disabled = false;
        if (!ok) {
            Popup.feedback('That did not go through. Try again, or mail us at hello@jobrunr.io.', 'error');
            return;
        }

        track('tour-trial-sent', { step: LAST_STEP });
        const slot = $('tour-popup-actions');
        if (slot) {
            const done = document.createElement('p');
            done.className = 'tour-auth__lead';
            done.textContent = 'On its way. A human from the team will follow up at ' + email + ' with your trial license.';
            slot.replaceChildren(done);
            Popup.reflow();
        }
    }

    function linkAction(icon, label, href, quiet) {
        const link = document.createElement('a');
        link.className = 'tour-action' + (quiet ? ' tour-action--quiet' : '');
        link.href = href;
        if (href.startsWith('http')) { link.target = '_blank'; link.rel = 'noopener'; }
        link.innerHTML = '<i class="' + icon + '"></i><span>' + escapeHtml(label) + '</span>';
        link.addEventListener('click', () => track('tour-finale-click', { href: href }));
        return link;
    }

    function signInForm() {
        const form = document.createElement('form');
        form.className = 'tour-auth';
        form.innerHTML =
            '<p class="tour-auth__lead">This button writes to the bank, so we card you at the door: leave ' +
            'an email, click the link it sends, and this tab unlocks itself. No password to invent.</p>' +
            '<input class="tour-auth__field" type="email" name="email" placeholder="you@company.com" required autocomplete="email">' +
            '<input class="tour-auth__field" type="text" name="name" placeholder="Your name" autocomplete="name" hidden>' +
            '<input class="tour-auth__field" type="text" name="company" placeholder="Company" autocomplete="organization" hidden>' +
            '<div class="tour-auth__row">' +
            '<button class="tour-btn tour-btn--primary" type="submit">Email me a link</button>' +
            '<button class="tour-link" type="button" data-cancel>Not now</button>' +
            '</div>';

        form.querySelector('[data-cancel]').addEventListener('click', () => Popup.prepareAndShow());
        form.addEventListener('submit', event => {
            event.preventDefault();
            submitMagicLink(form);
        });
        return form;
    }

    async function submitMagicLink(form) {
        const submit = form.querySelector('button[type="submit"]');
        const email = form.elements.email.value.trim();
        const name = form.elements.name.value.trim();

        submit.disabled = true;
        Popup.feedback(null);
        Auth.rememberReturn();

        let result;
        try {
            const response = await fetch('/tour/magic-link', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ email: email, name: name, company: form.elements.company.value.trim() })
            });
            if (response.status === 429) {
                Popup.feedback(await rateLimitMessage(response), 'error');
                submit.disabled = false;
                return;
            }
            result = await response.json();
        } catch (e) {
            Popup.feedback('Could not reach the demo. Try again in a moment.', 'error');
            submit.disabled = false;
            return;
        }

        submit.disabled = false;
        if (result.status === 'needs-registration') {
            form.elements.name.hidden = false;
            form.elements.company.hidden = false;
            form.elements.name.focus();
            Popup.feedback('First time here. Your name completes the sign-up.', null);
            return;
        }
        if (result.status !== 'sent') {
            Popup.feedback(result.message || 'That email did not work.', 'error');
            return;
        }

        track('tour-login-sent', { step: Tour.current });
        const slot = $('tour-popup-actions');
        if (slot) {
            const waiting = document.createElement('p');
            waiting.className = 'tour-auth__lead';
            waiting.textContent = 'Sent to ' + email + '. Open the link, then come back to this tab: it unlocks by itself.';
            slot.replaceChildren(waiting);
            Popup.reflow();
        }
        Auth.watch(() => {
            Toast.show('You are in. Everything is unlocked.');
            Popup.prepareAndShow();
        });
    }

    /* ------------------------------------------------------------------ actions */

    async function runAction(step, action, button) {
        if (Auth.blocks(action)) { Popup.askToSignIn(); return; }

        const label = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i><span>Running…</span>';
        Popup.feedback(null);
        track('tour-action-run', { step: step.number, url: action.url });

        try {
            const response = await fetch(action.url, {
                method: action.method,
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            });
            await reportActionResult(response, step);
        } catch (e) {
            Popup.feedback('Could not reach JobRunr Finance. Try again in a moment.', 'error');
        }

        button.disabled = false;
        button.innerHTML = label;
    }

    async function reportActionResult(response, step) {
        if (response.status === 429) {
            Popup.feedback(await rateLimitMessage(response), 'error');
            return;
        }
        // A gated GET quietly lands on the login page instead of failing, so check where we ended up.
        if (response.status === 401 || response.status === 403 || landedOnLogin(response)) {
            await Auth.refresh();
            Popup.prepareAndShow();
            Popup.askToSignIn();
            return;
        }

        const body = await readBody(response);
        if (!response.ok) {
            const hint = 'Steps 1 and 2 of this tour create the cards the later ones need.';
            Popup.feedback(body.message ? body.message + ' ' + hint : 'JobRunr Finance could not run that. ' + hint, 'error');
            return;
        }

        Popup.feedback(body.message || 'Done. Watch the dashboard.', body.ok === false ? null : 'success');
        if (body.ok !== false) {
            Beacon.flash();
            Toast.show('Jobs are on their way to the dashboard.');
            await refreshStage(step);
        }
    }

    /**
     * The dashboard does not refetch a list it is already showing, so after a trigger ran the stage
     * reloads on the step's own page and the new jobs walk in without anyone hunting for a refresh
     * button. A reload leaves no history entry behind, which keeps invariant 1 intact.
     */
    async function refreshStage(step) {
        if (!step.anchor) return;
        const win = Stage.win;
        if (win && sameLocation(Stage.here(), step.anchor.page)) {
            win.location.reload();
            await Stage.loaded();
        } else {
            await Stage.navigate(step.anchor.page);
        }
        await Tour.reanchor();
    }

    function landedOnLogin(response) {
        if (!response.url) return false;
        try { return new URL(response.url).pathname.startsWith('/login'); } catch (e) { return false; }
    }

    async function readBody(response) {
        const type = response.headers.get('Content-Type') || '';
        if (!type.includes('json')) return {};
        try { return await response.json(); } catch (e) { return {}; }
    }

    async function rateLimitMessage(response) {
        const body = await readBody(response);
        return body.message || 'That was a lot of demo traffic, even for a bank. Give it a minute and try again.';
    }

    /* ------------------------------------------------------------------ pill */

    const Pill = {
        node: $('tour-pill'),
        count: $('tour-pill-count'),
        title: $('tour-pill-title'),
        toggle: $('tour-pill-toggle'),
        jump: $('tour-jump'),
        resume: $('tour-pill-show'),
        back: $('tour-pill-back'),

        show() { this.node.hidden = false; },

        update(step) {
            this.count.textContent = 'Step ' + step.number + ' of ' + DATA.totalSteps;
            this.title.textContent = step.title;

            this.node.querySelectorAll('.tour-pill__act').forEach(actNode => {
                const act = DATA.acts[Number(actNode.dataset.act)];
                const size = act.lastStep - act.firstStep + 1;
                const done = DATA.steps
                    .filter(candidate => candidate.act === act.index && Progress.has(candidate.number)).length;
                actNode.querySelector('.tour-pill__act-fill').style.width = Math.round((done / size) * 100) + '%';
                actNode.classList.toggle('is-current', act.index === step.act);
            });

            this.jump.querySelectorAll('.tour-jump__step').forEach(button => {
                const number = Number(button.dataset.step);
                button.classList.toggle('is-current', number === step.number);
                button.classList.toggle('is-done', Progress.has(number));
            });
        },

        openJump(open) {
            this.jump.hidden = !open;
            this.node.classList.toggle('is-open', open);
            this.toggle.setAttribute('aria-expanded', String(open));
        },

        showResume(show) { this.resume.hidden = !show; },
        showBack(show) { this.back.hidden = !show; }
    };

    /* ------------------------------------------------------------------ off-track watch */

    const OffTrack = {
        timer: null,
        step: null,
        away: false,

        watch(step) {
            this.stop();
            if (!step.anchor) return;
            this.step = step;
            this.away = false;
            this.timer = setInterval(() => this.check(), OFF_TRACK_TICK);
        },

        stop() {
            if (this.timer) { clearInterval(this.timer); this.timer = null; }
            this.away = false;
            Pill.showBack(false);
        },

        check() {
            const path = Stage.path();
            if (!path) return;

            const wanted = new URL(this.step.anchor.page, location.origin).pathname;
            const onTrack = this.step.anchor.match === 'prefix' ? path.startsWith(wanted) : path === wanted;
            if (onTrack === !this.away) return;

            this.away = !onTrack;
            Pill.showBack(this.away);
            if (this.away) {
                Beacon.detach();
                Popup.minimize();
                Pill.showResume(false);
                track('tour-offtrack', { step: this.step.number });
            } else {
                Tour.reanchor();
            }
        }
    };

    /* ------------------------------------------------------------------ the tour itself */

    const Tour = {
        current: FIRST_STEP,
        token: 0,

        async open(number, options) {
            const step = STEPS.get(number);
            if (!step) return;
            const settings = options || {};
            const token = ++this.token;

            this.current = number;
            Progress.visit(number);
            history[settings.replace ? 'replaceState' : 'pushState']({ step: number }, '', '/tour/step/' + number + location.search);

            AppPanel.close();
            CodePanel.close();
            OffTrack.stop();
            Pill.show();
            Pill.update(step);
            Pill.openJump(false);
            Popup.prepare(step);
            track('tour-step-view', { step: number, title: step.title });

            // Show the narration even when the dashboard is slow: never make anyone wait to read.
            const settle = setTimeout(() => { if (token === this.token) Popup.show(null); }, POPUP_SETTLE);

            if (!step.anchor) {
                clearTimeout(settle);
                Beacon.detach();
                Popup.show(null);
                return;
            }

            await Stage.navigate(step.anchor.page);
            if (token !== this.token) return;

            const target = await resolveAnchor(step.anchor);
            if (token !== this.token) return;

            clearTimeout(settle);
            if (target) Beacon.attach(target); else Beacon.detach();
            Popup.show(Beacon.point);
            OffTrack.watch(step);
        },

        /** After a snap-back the dashboard has re-rendered, so the anchor has to be found again. */
        async reanchor() {
            const step = STEPS.get(this.current);
            if (!step || !step.anchor) return;
            const token = ++this.token;
            const target = await resolveAnchor(step.anchor);
            if (token !== this.token) return;
            if (target) Beacon.attach(target); else Beacon.detach();
            Popup.show(Beacon.point);
        },

        async takeMeBack() {
            const step = STEPS.get(this.current);
            track('tour-offtrack-snapback', { step: this.current });
            OffTrack.stop();
            await Stage.navigate(step.anchor.page);
            await this.reanchor();
            OffTrack.watch(step);
        },

        finish() {
            track('tour-completed', { steps: Progress.completed.length });
            Beacon.detach();
            OffTrack.stop();
            Popup.showFinale();
        }
    };

    /* ------------------------------------------------------------------ intro */

    const Intro = {
        node: $('tour-intro'),
        start: $('tour-start'),
        restart: $('tour-restart-intro'),

        show() {
            ROOT.classList.add('is-intro');
            this.node.hidden = false;
            const resume = Progress.introSeen && Progress.lastStep > FIRST_STEP;
            if (resume) {
                this.start.textContent = 'Resume at step ' + Progress.lastStep;
                this.restart.hidden = false;
            }
        },

        hide() {
            ROOT.classList.remove('is-intro');
            this.node.hidden = true;
        },

        enter(number) {
            Progress.introDone();
            this.hide();
            track('tour-started', { step: number });
            Tour.open(number, { replace: true });
        }
    };

    /* ------------------------------------------------------------------ wiring */

    Beacon.onMove = () => Popup.reflow();

    Pill.toggle.addEventListener('click', () => Pill.openJump(Pill.jump.hidden));
    Pill.resume.addEventListener('click', () => Popup.restore());
    Pill.back.addEventListener('click', () => Tour.takeMeBack());

    $('tour-app-close').addEventListener('click', () => AppPanel.close());
    $('tour-code-close').addEventListener('click', () => CodePanel.close());

    $('tour-jump').querySelectorAll('.tour-jump__step').forEach(button => {
        button.addEventListener('click', () => Tour.open(Number(button.dataset.step)));
    });

    $('tour-restart').addEventListener('click', () => {
        Progress.reset();
        Pill.openJump(false);
        Tour.open(FIRST_STEP);
    });

    Intro.start.addEventListener('click', () => {
        const resume = Progress.introSeen && Progress.lastStep > FIRST_STEP;
        Intro.enter(resume ? Progress.lastStep : FIRST_STEP);
    });
    Intro.restart.addEventListener('click', () => { Progress.reset(); Intro.enter(FIRST_STEP); });

    window.addEventListener('popstate', () => {
        const number = stepFromUrl();
        if (number) Tour.open(number, { replace: true });
    });

    window.addEventListener('resize', () => Popup.reflow());

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            if (!CodePanel.node.hidden) return CodePanel.close();
            if (!AppPanel.node.hidden) return AppPanel.close();
            if (!Pill.jump.hidden) return Pill.openJump(false);
            Popup.minimize();
        }
    });

    // The dashboard scrolls inside the iframe, so the beacon has to hear about it from in there.
    Stage.frame.addEventListener('load', () => {
        const doc = Stage.doc;
        if (doc) doc.addEventListener('scroll', () => Beacon.tick(), true);
    });

    document.addEventListener('visibilitychange', () => { if (!document.hidden) Beacon.tick(); });

    // ?tourDebug=1 also hands the console a handle, which is how the anchor sweep after a JobRunr Pro
    // upgrade is done: open every step, see which selector each one resolved to.
    if (DEBUG) {
        window.jobrunrTour = {
            data: DATA,
            open: number => Tour.open(number),
            anchored: () => Boolean(Beacon.target),
            resolved: () => Beacon.target ? Beacon.target.id || Beacon.target.tagName.toLowerCase() : null
        };
    }

    function stepFromUrl() {
        const match = location.pathname.match(/^\/tour\/step\/(\d+)/);
        return match ? Number(match[1]) : null;
    }

    /* ------------------------------------------------------------------ boot */

    (function boot() {
        DashboardColumns.showLabels();

        const deepLinked = stepFromUrl();
        const step = STEPS.get(deepLinked) || STEPS.get(Progress.lastStep) || STEPS.get(FIRST_STEP);
        Stage.frame.src = step.anchor ? step.anchor.page : '/dashboard';

        if (deepLinked) {
            Intro.hide();
            Progress.introDone();
            Tour.open(step.number, { replace: true });
        } else {
            Intro.show();
        }
    })();
})();
