# lectern

Collection and analysis of addresses by members of the Church of Jesus Christ of Latter-day Saints.

## Install & Run

1. Download, then unzip an appropriate web driver:

   - [Chrome](https://chromedriver.chromium.org/downloads)
   - [Firefox](https://github.com/mozilla/geckodriver/releases)
   
2. Add the path to the driver to the VM options when running an app scraper:

   - Chrome: `-Dwebdriver.chrome.driver=/path/to/chromedriver`
   - Firefox: `-Dwebdriver.gecko.driver=/path/to/geckodriver`

### General Conference

Run these programs, in order:
```
GeneralConferenceScraper
GeneralConferenceAddressScraper
```
