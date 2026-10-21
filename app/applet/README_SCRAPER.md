# NexusAI Scraper Backend

This project now uses a dedicated Node.js backend to scrape responses from `notrack.ai` (for Chat) and `deepai.org` (for Images) directly using Playwright. 

This replaces the old `freetxtapi` endpoints.

## How it works
The Android app is configured to send POST requests to:
- `http://10.0.2.2:3000/chat` (Step 1 & 3 - Notrack Chat Scraper)
- `http://10.0.2.2:3001/generate-image` (Step 4 - DeepAI Standalone Image Scraper)
*(10.0.2.2 is the Android emulator's loopback address to your host environment).*

## Running the Scrapers locally

If you export this project and run it on your own machine, you can run both scraper servers alongside the Android app.

1. Ensure you have Node.js installed.
2. Install the dependencies and Playwright browsers:
   ```bash
   npm install
   npx playwright install chromium
   ```
3. Start the services:
   - To run both simultaneously:
     ```bash
     npm run start:both
     ```
   - Or run individually:
     ```bash
     node notrack_scraper_server.js      # Port 3000 (Chat)
     node deepai_image_scraper_server.js # Port 3001 (Images)
     ```

The server will start on port 3000. Your Android emulator will now be able to generate scripts and images via the scraper.

## Deployment
If you want to run this app on a physical Android device, you must deploy this Node.js server to a cloud provider (e.g., Render, Heroku, Google Cloud Run) and update the `urlsToTry` array in `YouTubeDashboardScreen.kt` with your live public URL.
