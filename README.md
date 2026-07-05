# LumoDroid Your Private Android Assistant

LumoDroid is a powerful AI-driven Android assistant that runs entirely on your device, connecting to the Lumo AI API for natural language understanding and reasoning. 
It can execute real system actions send SMS, make calls, manage files, run shell commands, edit images, and much more all through natural conversation.

---

## Table of Contents

1. [Features Overview](#features-overview)
2. [Capabilities & Tools](#capabilities--tools)
3. [Installation](#installation)
4. [Configuration](#configuration)
5. [Termux Integration](#termux-integration)
6. [App Shortcuts](#app-shortcuts)
7. [Home Screen Widget](#home-screen-widget)
8. [Privacy & Permissions](#privacy--permissions)
9. [Troubleshooting](#troubleshooting)

---

## Features Overview

- **Conversational AI** — Chat with a powerful LLM that can reason, plan, and execute multi-step tasks
- **Real System Actions** — Send SMS, make phone calls, set alarms, create calendar events, launch apps
- **File Management** — Read, search, and list files with recursive directory scanning and folder size calculation
- **Web Search & Fetch** — Search the web and extract content from any URL
- **Image Generation & Editing** — Generate new images from text prompts or semantically edit attached images
- **PDF Extraction** — Extract text from PDF files on-device
- **Shell Commands** — Run shell commands with automatic routing to Termux for advanced networking tools
- **Network Diagnostics** — Detailed Wi-Fi, mobile data, and connectivity information
- **Device Information** — Hardware specs, battery, sensors, and system details
- **Clipboard Access** — Read and write the system clipboard
- **GPS Location** — Get current device location
- **Contacts** — Search device contacts
- **App Management** — List installed apps and launch them by name or package
- **Animated Splash Screen** — Polished launch experience with pulsing logo and tagline
- **Home Screen Widget** — Quick access widget on your home screen
- **App Shortcuts** — Long-press the app icon for quick actions (Quick Ask, Scan Network, Check SMS)
- **Selectable Text** — All chat bubble text is selectable and copyable
- **Cancellation Support** — Stop AI generation mid-response with the stop button
- **Dark Theme** — Clean, modern dark UI throughout

---

## Capabilities & Tools

LumoDroid comes with the following tools that the AI can use autonomously:

### Web & Information
| Tool | Description |
|------|-------------|
| `web_search` | Search the web for real-time information |
| `web_fetch` | Extract and read content from any web page URL |
| `generate_image` | Create a new image from a text description |
| `edit_image` | Semantically edit an attached image (e.g., "add a hat", "change background") |

### Communication
| Tool | Description |
|------|-------------|
| `read_sms` | Read recent SMS messages from the inbox |
| `send_sms` | Send an SMS message to a contact or phone number |
| `contacts` | Search and list device contacts by name or number |
| `make_call` | Initiate a phone call (direct call if permission granted, dialer otherwise) |
| `share_text` | Share text via Android's share sheet to any compatible app |

### Apps & System
| Tool | Description |
|------|-------------|
| `list_apps` | List all installed applications with package names |
| `launch_app` | Launch an app by package name or display label |
| `set_alarm` | Set an alarm via the Android clock app |
| `set_timer` | Set a timer via the Android clock app |
| `create_calendar_event` | Create a calendar event with title, time, and description |
| `open_url` | Open a URL in the default browser |
| `open_device_settings` | Open specific Android settings pages (Wi-Fi, Bluetooth, Location, etc.) |

### Files & Storage
| Tool | Description |
|------|-------------|
| `read_file` | Read the contents of any file on the device |
| `search_files` | Search for files by name pattern across storage |
| `list_files` | List files in a directory with recursive option, folder size calculation, and size filtering |
| `extract_pdf_text` | Extract text content from PDF files on-device |

### Network & Connectivity
| Tool | Description |
|------|-------------|
| `get_network_info` | Get detailed network status including Wi-Fi details (SSID, RSSI, link speed, frequency), mobile data info (carrier, network type), VPN status, and bandwidth |
| `get_device_info` | Get hardware and system information (model, Android version, battery, sensors, display, memory) |
| `run_shell` | Execute shell commands; advanced tools auto-routed to Termux |
| `get_location` | Get current GPS coordinates and address |

### Utilities
| Tool | Description |
|------|-------------|
| `clipboard_read` | Read the current contents of the system clipboard |
| `clipboard_write` | Write text to the system clipboard |

---

## Installation

### Prerequisites

- Android device running **Android 8.0 (API 26)** or higher
- A **Lumo API key** 
- Optional: **Termux** installed for advanced networking commands

### Build from Source

```bash
# Clone the repository
git clone <repo-url>
cd LumoDroid

# Build the debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Install Pre-built APK

1. Transfer the `app-release.apk` file to your Android device
2. Open the file manager and tap on the APK
3. Allow installation from unknown sources if prompted
4. Follow the on-screen instructions to install

---

## Configuration

### 1. Set Your API Key

On first launch:

1. Open LumoDroid
2. Tap the **gear icon** (⚙) in the top-right corner
3. Enter your **Lumo API key** in the input field
4. Tap **Save**
5. You're ready to chat!

> **Note:** Without an API key, you will see an error message when trying to send a message. The key is stored locally on your device and never transmitted anywhere except directly to the Lumo API.

### 2. Grant Permissions

LumoDroid will automatically request the following runtime permissions on first launch:

- **SMS** (Read & Send) — For reading and sending SMS messages
- **Contacts** — For searching contacts
- **Location** (Fine & Coarse) — For GPS location queries
- **Notifications** — For Android 13+ notification posting
- **Call Phone** — For making phone calls
- **Storage** — For file access (Write for Android ≤ 9)
- **Wi-Fi State** — For detailed Wi-Fi network information

#### All Files Access (Optional but Recommended)

For full file management capabilities (recursive directory listing with folder sizes):

1. When prompted, you'll be taken to the **"All Files Access"** settings page
2. Find **LumoDroid** in the list and toggle **"Allow access to manage all files"**
3. Return to the app

> Without this permission, file operations are limited to app-specific directories and may fail for broader filesystem scans.

---

## Termux Integration

LumoDroid integrates with [Termux](https://termux.dev/) to provide advanced networking and system tools that aren't available in Android's default shell.

### Setup

#### Step 1: Install Termux

Download Termux from [F-Droid](https://f-droid.org/packages/com.termux/) (recommended) or the [GitHub Releases](https://github.com/termux/termux-app/releases).

> **⚠ Important:** Do NOT install Termux from the Google Play Store  it is outdated and non-functional.

#### Step 2: Enable External App Access

1. Open **Termux**
2. Create or edit the Termux properties file:
   ```bash
   nano ~/.termux/termux.properties
   ```
3. Add or uncomment this line:
   ```
   allow-external-apps=true
   ```
4. Save (`Ctrl+O`, `Enter`) and exit (`Ctrl+X`)
5. Fully close Termux (type `exit` until it closes, or swipe it away from recent apps)
6. Reopen Termux to apply changes

#### Step 3: Grant Display Over Other Apps

Termux needs this to display command sessions when invoked from LumoDroid:

1. Go to **Settings → Apps → Termux → Display over other apps**
2. Toggle **"Allow display over other apps"** to **ON**

#### Step 4: Grant RUN_COMMAND Permission

This permission allows LumoDroid to send commands to Termux. It is a custom permission defined by Termux with `dangerous` protection level. It should be granted automatically when both apps are installed, but if you see an error:

```bash
# Via ADB
adb shell pm grant com.lumodroid com.termux.permission.RUN_COMMAND
```

Or reinstall LumoDroid after Termux is installed — the permission request will fire automatically.

### How It Works

When you ask LumoDroid to run a command like `dig`, `nslookup`, `whois`, `nmap`, or `traceroute`:

1. LumoDroid detects the command isn't available in its sandbox
2. It routes the command to Termux via the `RunCommandService` intent
3. The command executes in Termux's background shell
4. Output is written to a shared file (`/sdcard/LumoDroid/termux_results/`)
5. LumoDroid reads the output and displays it in the chat

### Supported Termux Commands

The following commands are automatically routed to Termux when not available natively:

| Category | Commands |
|----------|----------|
| **DNS** | `dig`, `nslookup`, `whois` |
| **Network Scanning** | `nmap`, `traceroute`, `tcpdump`, `ss`, `ip`, `ifconfig`, `arp`, `route` |
| **File Transfer** | `curl`, `wget`, `ssh`, `scp`, `rsync` |
| **Media** | `ffmpeg`, `convert` (ImageMagick) |
| **Programming** | `python`, `pip`, `node`, `npm`, `ruby`, `perl`, `php`, `git` |
| **System** | `lsof`, `strace`, `htop`, `iftop`, `nethogs`, `iptables` |
| **Archive** | `tar` |

### Installing Additional Termux Packages

Some tools like `whois` need to be installed in Termux first:

```bash
# Open Termux and install the package
pkg install whois

# Other useful packages
pkg install nmap traceroute curl wget git python nodejs ffmpeg
```

When a command isn't installed, LumoDroid will detect the "command not found" error and suggest the correct `pkg install` command.

---

## App Shortcuts

LumoDroid supports Android app shortcuts — long-press the app icon to reveal:

| Shortcut | Action |
|----------|--------|
| **Quick Ask** | Opens the app with the chat input focused, ready to type |
| **Scan Network** | Automatically sends a network diagnostic prompt — runs connectivity checks, pings hosts, and summarizes results |
| **Check SMS** | Automatically reads and summarizes your recent SMS messages |

### Adding Shortcuts to Home Screen

1. Long-press the LumoDroid app icon
2. Long-press one of the shortcut options (e.g., "Quick Ask")
3. Drag it to an empty space on your home screen
4. Now you have one-tap access to that action

---

## Home Screen Widget

LumoDroid includes a home screen widget for quick access.

### Adding the Widget

1. Long-press an empty area on your home screen
2. Select **Widgets** (or find the Widgets option in your launcher)
3. Scroll to find **LumoDroid**
4. Drag the widget to your home screen
5. Resize as needed

### Widget Features

- Displays the LumoDroid logo and name
- Tapping the widget opens LumoDroid directly to the chat screen
- Minimalist design that matches the app's dark theme

---

## Privacy & Permissions

### Data Collection

LumoDroid **does not collect, store, or transmit** any personal data to third parties. Here's how data flows:

- **Chat messages** are sent directly to the Lumo AI API for processing
- **API key** is stored locally in Android's encrypted SharedPreferences
- **Files, SMS, contacts, location** are accessed only when you explicitly ask the AI to use them
- **No analytics, no telemetry, no tracking**

### Permission Justifications

| Permission | Why It's Needed |
|------------|-----------------|
| `INTERNET` | Connect to Lumo AI API |
| `ACCESS_NETWORK_STATE` | Check network connectivity status |
| `ACCESS_WIFI_STATE` | Read Wi-Fi details (SSID, signal strength, etc.) |
| `CHANGE_WIFI_STATE` | Potential future Wi-Fi scanning features |
| `READ_SMS` | Read SMS messages when user asks |
| `SEND_SMS` | Send SMS messages when user asks |
| `READ_CONTACTS` | Search contacts when user asks |
| `CALL_PHONE` | Make phone calls when user asks |
| `ACCESS_FINE_LOCATION` | Get GPS coordinates when user asks |
| `ACCESS_COARSE_LOCATION` | Approximate location when fine isn't available |
| `POST_NOTIFICATIONS` | Display service notifications (Android 13+) |
| `MANAGE_EXTERNAL_STORAGE` | Full file system access for file management tools |
| `READ_EXTERNAL_STORAGE` | Read files (Android ≤ 12) |
| `WRITE_EXTERNAL_STORAGE` | Write files (Android ≤ 9) |
| `READ_MEDIA_IMAGES` | Read image files |
| `READ_MEDIA_VIDEO` | Read video files |
| `READ_MEDIA_AUDIO` | Read audio files |
| `QUERY_ALL_PACKAGES` | List installed apps |
| `SET_ALARM` | Set alarms via Android clock app |
| `FOREGROUND_SERVICE` | Run background tasks for shell commands |
| `com.termux.permission.RUN_COMMAND` | Send commands to Termux |

---

## Troubleshooting

### "No API key set"

Open **Settings** (gear icon) and enter your Lumo API key, then tap **Save**.

### "WiFi details unavailable: WifiService permission denied"

The `ACCESS_WIFI_STATE` permission wasn't granted. Reinstall the app or grant it manually:
```bash
adb shell pm grant com.lumodroid android.permission.ACCESS_WIFI_STATE
```

### Termux Commands Not Working

**Symptom:** "Failed to send command to Termux" or "Not allowed to start service"

**Solutions:**

1. Ensure Termux is installed (from F-Droid, NOT Play Store)
2. Verify `allow-external-apps=true` in `~/.termux/termux.properties`
3. Restart Termux completely after changing properties
4. Grant display-over-other-apps permission to Termux
5. Grant the RUN_COMMAND permission:
   ```bash
   adb shell pm grant com.lumodroid com.termux.permission.RUN_COMMAND
   ```

### "command not found" in Termux Output

The tool isn't installed in Termux. Open Termux and install it:
```bash
pkg install <tool-name>
```

Common installations:
```bash
pkg install whois      # WHOIS lookups
pkg install nmap       # Network scanner
pkg install traceroute # Network path tracing
pkg install curl       # HTTP requests
pkg install dig        # DNS lookups (part of bind-utils or dnsutils)
```

### App Icon Not Updating After Install

Android caches launcher icons. To refresh:

1. Go to **Settings → Apps → Your Launcher → Force Stop**
2. Reopen the launcher

Or simply **reboot your device**.

### Widget "Could Not Add Widget"

This usually indicates a layout issue. Ensure you're running the latest version of LumoDroid. If the issue persists, remove the old widget and add a new one.

### File Operations Return "Permission Denied"

Grant **All Files Access**:

1. Go to **Settings → Apps → LumoDroid → All Files Access**
2. Toggle to **ON**
3. Return to the app and try again

### AI Can't See Attached Images

Make sure you're attaching images via the paperclip icon or sharing them to LumoDroid. The AI can see and analyze images directly, and can edit them using the `edit_image` tool.

---

## Technical Details

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** MVVM with ViewModel + StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 (API 34)
- **AI Backend:** Lumo AI API (OpenAI-compatible streaming)
- **Image Loading:** Coil
- **PDF Processing:** PDFBox-Android
- **Networking:** OkHttp + Gson
- **HTML Parsing:** Jsoup

---

## License

MIT

---

*LumoDroid — Your Android Private Assistant, Powered by Lumo*
