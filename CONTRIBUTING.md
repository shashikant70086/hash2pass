# Contributing to hash2pass

First off, thank you for considering contributing to **hash2pass**! It's people like you that make open source such a great community to learn, inspire, and create.

## Code of Conduct

By participating in this project, you are expected to uphold a welcoming and inclusive environment. Please be respectful to all contributors.

## How Can I Contribute?

### Reporting Bugs
If you find a bug, please create an issue on GitHub with:
- A clear and descriptive title.
- Steps to reproduce the issue.
- Your Android device model and OS version.
- Any relevant logs or screenshots.

### Suggesting Enhancements
Have an idea for a new feature? We'd love to hear it! Open an issue describing:
- What the feature is and how it works.
- Why it would be useful for the project.

### Submitting Pull Requests
1. **Fork the repository** and create your branch from `main`.
2. **Make your changes** in your feature branch.
3. **Test your code** to ensure it doesn't break existing functionality.
4. **Update documentation** if your changes affect the API, setup process, or UI.
5. **Issue a pull request**. Ensure your PR description clearly describes the problem and solution.

## Development Setup

To work on the Android app (`otp-gateway-android`):
1. Open the `otp-gateway-android` folder in Android Studio.
2. Sync Gradle files.
3. Build and run on a physical Android device (emulators cannot send real SMS).

To work on the tester (`otp-gateway-tester`):
1. Navigate to the `otp-gateway-tester` directory.
2. Edit `index.html`. No build step or Node.js server is required. Just serve it using any basic HTTP server (e.g., `python -m http.server`).

## License
By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
