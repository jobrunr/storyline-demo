# JobRunr Finance - Interactive Guide

This is a standalone Spring Boot application that provides an interactive, web-based guide to learning JobRunr Pro through a banking storyline.

## Features

- **Interactive Timeline**: Navigate through 12 steps grouped into 4 categories
- **Business-Focused Learning**: Each step presents a real business problem and solution
- **Dual View**: Toggle between code examples (from GitHub) and interactive demos
- **Progress Tracking**: Client-side progress saved in localStorage
- **HTMX-Powered**: Fast, modern interactions without heavy JavaScript frameworks
- **Pebble Templates**: Clean, simple templating
- **Bulma UI**: Beautiful, responsive design

## Tech Stack

- **Backend**: Spring Boot 3.5.6 with Java 21
- **Templates**: Pebble
- **Frontend**: HTMX, Bulma CSS, Bulma Timeline
- **Syntax Highlighting**: Highlight.js
- **Code Source**: GitHub (jobrunr/storyline-demo)

## Running the Application

```bash
# From the project root
./gradlew :demo-guide:bootRun

# Application will start at http://localhost:8082
```

## Project Structure

```
demo-guide/
├── src/main/
│   ├── java/org/jobrunr/guide/
│   │   ├── GuideApplication.java      # Main Spring Boot app
│   │   ├── GuideController.java       # Home and guide pages
│   │   ├── DemoController.java        # Interactive demo endpoints
│   │   └── StorylineStep.java         # Step definitions
│   └── resources/
│       ├── templates/
│       │   ├── index.peb              # Landing page
│       │   ├── guide.peb              # Main guide page
│       │   └── fragments/             # HTMX fragments
│       └── static/
│           ├── css/                   # Styles
│           └── js/                    # Client-side logic
└── build.gradle
```

## Pages

- `/` - Landing page with introduction and step overview
- `/guide` - Main interactive guide with timeline
- `/guide#step-N` - Direct link to specific step

## Development

The app uses Spring Boot DevTools for hot reload. Changes to templates and static assets are automatically picked up.

## No External Dependencies

All CSS and JS libraries are loaded via CDN:
- Bulma CSS
- Bulma Timeline extension
- HTMX
- Highlight.js
- Font Awesome

This keeps the build simple and eliminates the need for npm/webpack.
