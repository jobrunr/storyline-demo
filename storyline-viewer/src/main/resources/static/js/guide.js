// Progress tracking
const STORAGE_KEY = 'jobrunr-guide-progress';

// Initialize on page load
if (document.readyState === "loading") {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

function init() {
    loadProgress();
    updateProgressDisplay();
    hljs.highlightAll();

    // Re-initialize after HTMX loads new content
    document.body.addEventListener('htmx:afterSettle', function(event) {
        if (event.detail.target.id === 'step-content') {
            initializeTabSwitching();
            updateTimelineState();
            initializeCompleteButtons();
            hljs.highlightAll();
        }
    });

    // Initialize buttons on first load
    initializeCompleteButtons();
}

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

// Check if step is complete
function isStepComplete(stepNumber) {
    return getCompletedSteps().includes(stepNumber);
}

// Mark step as complete
function markStepComplete(stepNumber) {
    const completed = getCompletedSteps();
    if (!completed.includes(stepNumber)) {
        completed.push(stepNumber);
        saveProgress(completed);
        markStepVisually(stepNumber, 'completed');
        updateProgressDisplay();
        updateCompleteButton(stepNumber, true);

        // Show success notification
        showNotification(`Step ${stepNumber} completed!`);
    }
}

// Mark step as incomplete
function markStepIncomplete(stepNumber) {
    const completed = getCompletedSteps();
    const index = completed.indexOf(stepNumber);
    if (index > -1) {
        completed.splice(index, 1);
        saveProgress(completed);
        markStepVisually(stepNumber, 'active');
        updateProgressDisplay();
        updateCompleteButton(stepNumber, false);

        showNotification(`Step ${stepNumber} marked as incomplete`);
    }
}

// Update the complete/incomplete button state
function updateCompleteButton(stepNumber, isComplete) {
    const completeBtn = document.getElementById(`btn-complete-${stepNumber}`);
    const incompleteBtn = document.getElementById(`btn-incomplete-${stepNumber}`);
    if (completeBtn && incompleteBtn) {
        completeBtn.style.display = isComplete ? 'none' : 'inline-flex';
        incompleteBtn.style.display = isComplete ? 'inline-flex' : 'none';
    }
}

// Initialize complete buttons based on stored progress
function initializeCompleteButtons() {
    const stepContent = document.getElementById('step-content');
    const currentStep = stepContent?.querySelector('.content-screen[data-step]');
    if (currentStep) {
        const stepNumber = parseInt(currentStep.dataset.step);
        updateCompleteButton(stepNumber, isStepComplete(stepNumber));
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

// Update timeline visual state based on current step
function updateTimelineState() {
    const stepContent = document.getElementById('step-content');
    const currentStep = stepContent.querySelector('.content-screen');
    if (!currentStep) return;

    const stepNumber = parseInt(currentStep.dataset.step);
    const completed = getCompletedSteps();

    document.querySelectorAll('.timeline-item').forEach(item => {
        const itemStep = parseInt(item.dataset.step);
        item.classList.remove('active');
        if (itemStep === stepNumber) {
            item.classList.add('active');
        } else if (completed.includes(itemStep)) {
            item.classList.add('completed');
        }
    });
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

// Make functions globally available
window.markStepComplete = markStepComplete;
window.markStepIncomplete = markStepIncomplete;
window.isStepComplete = isStepComplete;
window.updateCompleteButton = updateCompleteButton;

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