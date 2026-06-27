# hash2pass

> Turn an Android phone into a self-hosted OTP service. SIM-based SMS, an HTTP API, no Firebase, no Twilio, no monthly bill.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./otp-gateway-android/LICENSE)

**hash2pass** is an open-source project that allows developers to avoid expensive SMS verification bills by using any Android phone with an active SIM card as a dedicated SMS gateway. It exposes a simple REST API that your backend can call to send and verify OTPs. 

## Project Structure

This repository is divided into two main components:

1. [**hash2pass Android App (`otp-gateway-android`)**](./otp-gateway-android/README.md): The core Android application that you install on your phone. It runs a lightweight HTTP server (Ktor) and handles sending SMS messages and validating OTP codes securely.
2. [**hash2pass Tester (`otp-gateway-tester`)**](./otp-gateway-tester/README.md): A lightweight, single-page web interface to easily test your Android gateway over the local network without writing any code.

## Why use hash2pass?

Building auth for a side project or college app and don't want:
- **A Twilio bill** (SMS costs add up quickly for thousands of users)
- **A Firebase Blaze plan** (Required for Phone Auth)
- **A PC running 24/7** to act as a gateway

Just plug in any Android phone with a Wi-Fi connection and an active SIM. The app exposes a tiny HTTP API. SMS goes out via the phone's native `SmsManager`.

## Getting Started

To get started, navigate to the component directories for specific instructions:
- [Android App Setup & API Docs](./otp-gateway-android/README.md)
- [Tester Setup](./otp-gateway-tester/README.md)
- [Contributing Guide](./CONTRIBUTING.md)

## License

This project is licensed under the [Apache 2.0 License](./otp-gateway-android/LICENSE).
