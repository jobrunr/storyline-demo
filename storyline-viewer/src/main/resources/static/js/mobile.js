(function () {
    const stack = document.getElementById('m-stack');
    const cards = Array.from(stack.querySelectorAll('.card'));
    const total = cards.length;
    const totalSteps = cards.filter(c => c.dataset.kind === 'title').length;

    const stepLabel = document.getElementById('m-step-label');
    const progressFill = document.getElementById('m-progress-fill');
    const dots = Array.from(document.querySelectorAll('#m-dots span'));
    const prevBtn = document.getElementById('m-prev');
    const nextBtn = document.getElementById('m-next');

    let current = 0;

    /* ---------- stack rendering ---------- */
    function render() {
        cards.forEach((card, i) => {
            const rel = i - current;
            let t, opacity, pe;
            if (rel < 0) {
                t = 'translateX(-130%) rotate(-16deg)'; opacity = 0; pe = 'none';
            } else if (rel === 0) {
                t = 'translate(0) rotate(0)'; opacity = 1; pe = 'auto';
            } else if (rel === 1) {
                t = 'translateY(14px) scale(0.94)'; opacity = 0.55; pe = 'none';
            } else if (rel === 2) {
                t = 'translateY(28px) scale(0.88)'; opacity = 0.28; pe = 'none';
            } else {
                t = 'translateY(28px) scale(0.88)'; opacity = 0; pe = 'none';
            }
            card.style.transform = t;
            card.style.opacity = opacity;
            card.style.pointerEvents = pe;
            card.style.zIndex = rel < 0 ? 0 : 100 - rel;
        });
        updateChrome();
    }

    function updateChrome() {
        const card = cards[current];
        const kind = card.dataset.kind;
        if (kind === 'intro') stepLabel.textContent = 'Welcome';
        else if (kind === 'trial') stepLabel.textContent = 'Free trial';
        else stepLabel.textContent = `Step ${card.dataset.step} of ${totalSteps}`;
        progressFill.style.width = `${((current + 1) / total) * 100}%`;
        dots.forEach(d => d.classList.toggle('is-active', d.dataset.kind === kind));
        prevBtn.disabled = current === 0;
        nextBtn.disabled = current === total - 1;
    }

    function go(delta) {
        const next = Math.min(total - 1, Math.max(0, current + delta));
        if (next !== current) { current = next; render(); }
    }

    function jumpToStep(stepNumber) {
        const idx = cards.findIndex(c => c.dataset.step == stepNumber && c.dataset.kind === 'title');
        if (idx >= 0) { current = idx; render(); }
    }

    /* ---------- drag (Tinder) ---------- */
    let startX = 0, startY = 0, axis = null, dragging = false, dragCard = null;
    const THRESHOLD = 90;

    stack.addEventListener('pointerdown', e => {
        const card = cards[current];
        if (!card.contains(e.target)) return;
        startX = e.clientX; startY = e.clientY; axis = null; dragging = false; dragCard = card;
    });

    stack.addEventListener('pointermove', e => {
        if (!dragCard) return;
        const dx = e.clientX - startX;
        const dy = e.clientY - startY;
        if (axis === null && Math.hypot(dx, dy) > 8) {
            axis = Math.abs(dx) > Math.abs(dy) ? 'x' : 'y';
            if (axis === 'x') { dragging = true; dragCard.classList.add('is-dragging'); }
        }
        if (dragging) {
            e.preventDefault();
            dragCard.style.transform = `translateX(${dx}px) rotate(${dx * 0.06}deg)`;
            dragCard.style.opacity = String(Math.max(0.4, 1 - Math.abs(dx) / 600));
        }
    });

    function endDrag(e) {
        if (!dragCard) return;
        const card = dragCard;
        dragCard = null;
        if (!dragging) return;
        dragging = false;
        card.classList.remove('is-dragging');
        const dx = e.clientX - startX;
        if (dx <= -THRESHOLD && current < total - 1) current++;
        else if (dx >= THRESHOLD && current > 0) current--;
        render();
    }

    stack.addEventListener('pointerup', endDrag);
    stack.addEventListener('pointercancel', endDrag);

    prevBtn.addEventListener('click', () => go(-1));
    nextBtn.addEventListener('click', () => go(1));

    /* ---------- chapters sheet ---------- */
    const sheet = document.getElementById('m-sheet');
    const scrim = document.getElementById('m-scrim');
    function openSheet() { sheet.classList.add('is-open'); scrim.classList.add('is-open'); sheet.setAttribute('aria-hidden', 'false'); }
    function closeSheet() { sheet.classList.remove('is-open'); scrim.classList.remove('is-open'); sheet.setAttribute('aria-hidden', 'true'); }
    document.getElementById('m-chapters-btn').addEventListener('click', openSheet);
    scrim.addEventListener('click', closeSheet);
    sheet.querySelectorAll('.chapter__step[data-jump]').forEach(btn => {
        btn.addEventListener('click', () => { jumpToStep(btn.dataset.jump); closeSheet(); });
    });

    /* ---------- dashboard fullscreen overlay ---------- */
    const fs = document.getElementById('m-fs');
    const fsBody = document.getElementById('m-fs-body');

    function openFs(src) {
        const iframe = document.createElement('iframe');
        iframe.src = src;
        iframe.title = 'JobRunr dashboard';
        iframe.setAttribute('data-clarity-unmask', 'true');
        fsBody.replaceChildren(iframe);
        fs.classList.add('is-open');
        fs.setAttribute('aria-hidden', 'false');
    }
    function closeFs() {
        fs.classList.remove('is-open');
        fs.setAttribute('aria-hidden', 'true');
        fsBody.replaceChildren();
    }
    document.querySelectorAll('[data-fullscreen]').forEach(btn => {
        btn.addEventListener('click', () => {
            const iframe = btn.closest('.card').querySelector('.card__frame iframe');
            if (iframe) openFs(iframe.src);
        });
    });
    document.getElementById('m-fs-close').addEventListener('click', closeFs);
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeFs(); });

    /* ---------- free-trial sign-up ---------- */
    const TRIAL_KEY = 'jobrunr-trial-submitted';

    function utmParams() {
        const p = new URLSearchParams(location.search);
        return {
            utm_source: p.get('utm_source') || '',
            utm_medium: p.get('utm_medium') || '',
            utm_campaign: p.get('utm_campaign') || '',
            utm_term: p.get('utm_term') || '',
            utm_content: p.get('utm_content') || ''
        };
    }

    function markTrialDone() {
        document.querySelectorAll('.card--trial').forEach(c => c.classList.add('is-done'));
    }

    if (localStorage.getItem(TRIAL_KEY)) markTrialDone();

    document.querySelectorAll('.trial-form').forEach(form => {
        form.addEventListener('submit', async e => {
            e.preventDefault();
            const card = form.closest('.card--trial');
            const input = form.querySelector('input[type="email"]');
            const btn = form.querySelector('button');
            const err = card.querySelector('.trial-error');
            const email = input.value.trim();
            if (!email || !email.includes('@')) { err.textContent = 'Please enter a valid email address.'; return; }
            err.textContent = '';
            const original = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            try {
                const res = await fetch('/m/trial', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, ...utmParams() })
                });
                if (!res.ok) throw new Error('bad status ' + res.status);
                localStorage.setItem(TRIAL_KEY, '1');
                markTrialDone();
            } catch (err2) {
                err.textContent = 'Something went wrong — please try again.';
                btn.disabled = false;
                btn.innerHTML = original;
            }
        });
    });

    /* ---------- code syntax highlighting ---------- */
    document.body.addEventListener('htmx:afterSettle', e => {
        const code = e.detail.target.querySelector('pre code:not([data-highlighted])');
        if (code && window.hljs) hljs.highlightElement(code);
    });

    render();
})();
