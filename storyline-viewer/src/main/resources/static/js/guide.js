// Progress tracking
const STORAGE_KEY = 'jobrunr-guide-progress';

// Initialize on page load
document.addEventListener('DOMContentLoaded', function () {
    loadProgress();
    updateProgressDisplay();
    initializeNavigation();
    initializeTabSwitching();

    // Handle initial hash or show welcome screen
    if (window.location.hash) {
        navigateToHash(window.location.hash);
    } else {
        showScreen('welcome-screen');
    }
});

// Load progress from localStorage
function loadProgress() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
        try {
            const progress = JSON.parse(saved);
            progress.completed.forEach(stepNum => {
                markStepVisually(stepNum, 'completed');
            });
        } catch (e) {
            console.error('Failed to load progress:', e);
        }
    }
}

// Save progress to localStorage
function saveProgress(completedSteps) {
    const progress = {
        completed: completedSteps,
        lastUpdated: new Date().toISOString()
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
}

// Get completed steps
function getCompletedSteps() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
        try {
            return JSON.parse(saved).completed || [];
        } catch (e) {
            return [];
        }
    }
    return [];
}

// Mark step as complete
function markStepComplete(stepNumber) {
    const completed = getCompletedSteps();
    if (!completed.includes(stepNumber)) {
        completed.push(stepNumber);
        saveProgress(completed);
        markStepVisually(stepNumber, 'completed');
        updateProgressDisplay();

        // Show success notification
        showNotification(`Step ${stepNumber} completed! 🎉`);
    }
}

// Update visual state of step
function markStepVisually(stepNumber, state) {
    const timelineItem = document.querySelector(`.timeline-item[data-step="${stepNumber}"]`);
    if (timelineItem) {
        timelineItem.classList.remove('active', 'completed');
        if (state) {
            timelineItem.classList.add(state);
        }
    }
}

// Update progress bar and text
function updateProgressDisplay() {
    const completed = getCompletedSteps();
    const total = 12;
    const progressBar = document.getElementById('progress-bar');
    const progressText = document.getElementById('progress-text');

    if (progressBar) {
        progressBar.value = completed.length;
    }
    if (progressText) {
        progressText.textContent = `${completed.length}/${total}`;
    }
}

// Navigation handling
function initializeNavigation() {
    window.addEventListener('hashchange', function () {
        navigateToHash(window.location.hash);
    });

    // Step links
    document.querySelectorAll('.step-link').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const hash = this.getAttribute('href');
            window.location.hash = hash;
        });
    });
}

// Navigate to a specific hash
function navigateToHash(hash) {
    if (!hash) return;

    const stepId = hash.substring(1); // Remove #
    const stepNumber = parseInt(stepId.split('-')[1]);

    if (stepNumber) {
        showStep(stepNumber);

        // Scroll timeline to active step
        const timelineItem = document.querySelector(`.timeline-item[data-step="${stepNumber}"]`);
        if (timelineItem) {
            timelineItem.scrollIntoView({behavior: 'smooth', block: 'center'});
        }
    }
}

// Show specific step
function showStep(stepNumber) {
    // Hide all screens
    document.querySelectorAll('.content-screen').forEach(screen => {
        screen.classList.remove('active');
    });

    // Show target screen
    const targetScreen = document.getElementById(`step-${stepNumber}`);
    if (targetScreen) {
        targetScreen.classList.add('active');
    }

    // Update timeline visual state
    document.querySelectorAll('.timeline-item').forEach(item => {
        const itemStep = parseInt(item.dataset.step);
        const completed = getCompletedSteps();

        item.classList.remove('active');
        if (itemStep === stepNumber) {
            item.classList.add('active');
        } else if (completed.includes(itemStep)) {
            item.classList.add('completed');
        }
    });

    // Scroll to top of content area
    document.querySelector('.content-area').scrollTop = 0;
}

// Show screen (for welcome)
function showScreen(screenId) {
    document.querySelectorAll('.content-screen').forEach(screen => {
        screen.classList.remove('active');
    });

    const screen = document.getElementById(screenId);
    if (screen) {
        screen.classList.add('active');
    }
}

// Tab switching
function initializeTabSwitching() {
    document.querySelectorAll('.tab-link').forEach(tab => {
        tab.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.dataset.tab;

            // Remove active from siblings
            this.parentElement.parentElement.querySelectorAll('.tab-link').forEach(t => {
                t.classList.remove('is-active');
            });

            // Add active to clicked tab
            this.classList.add('is-active');

            // Show corresponding content
            const parent = this.closest('.content-screen');
            parent.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('is-active');
            });

            const targetContent = document.getElementById(targetId);
            if (targetContent) {
                targetContent.classList.add('is-active');
            }
        });
    });
}

// Show notification
function showNotification(message) {
    const notification = document.createElement('div');
    notification.className = 'notification is-success is-light';
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';
    notification.style.animation = 'slideIn 0.3s';
    notification.innerHTML = `
        <button class="delete"></button>
        ${message}
    `;

    document.body.appendChild(notification);

    // Auto-remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s';
        setTimeout(() => notification.remove(), 300);
    }, 3000);

    // Remove on click
    notification.querySelector('.delete').addEventListener('click', () => {
        notification.remove();
    });
}

// Make markStepComplete globally available
window.markStepComplete = markStepComplete;

// Add animation keyframes
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);