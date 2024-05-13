# Fallout1DemoAI

Install Godot Engine:

Download and install the latest version of Godot Engine from the official website. Godot provides builds for both Windows and Linux, so you can easily set it up on your respective systems.
Kotlin for Godot:

For Kotlin support, you'll need to integrate the Kotlin/JVM module for Godot, which allows you to write your game logic in Kotlin. This can be found on GitHub under the Godot Kotlin project. Follow the setup instructions carefully for both Windows and Ubuntu environments.
Ensure you have a compatible JDK installed on both systems for Kotlin development.
OpenAI API Setup:

Register an account with OpenAI and obtain API keys for GPT-3.5 turbo access. This will enable you to integrate AI-driven dialogues and NPC behavior.
Install any necessary libraries for making HTTPS requests in Kotlin, such as Ktor or OkHttp, to facilitate communication with the OpenAI API.
IDE Setup:

For Kotlin development, IntelliJ IDEA is highly recommended. It integrates well with Kotlin and supports both Windows and Linux. Install IntelliJ IDEA on both systems.
Configure IntelliJ IDEA with the JDK you're using and ensure it recognizes your Kotlin projects.
Version Control:

Install Git if it’s not already set up. Use Git for version control to manage your project code, especially when switching between Windows and Ubuntu environments.
GitHub or GitLab can be used to host your repository, enabling easy code sharing and collaboration.
Test OpenAI API Connectivity:

Before diving into the development, make a simple command-line application in Kotlin that makes an API request to OpenAI and outputs the response. This step ensures that your API access and HTTP client setup are functional.
Learning Resources:

Familiarize yourself with the Godot documentation and any tutorials on integrating external libraries with it.
The Kotlin language documentation will be invaluable for specific Kotlin-related questions.
Review OpenAI’s API documentation for guidelines on making requests and understanding response formats.
2. Start Small
   Begin with a small, manageable scope:

Create a basic Godot project: Start by setting up a new project in Godot. Create a simple scene to verify that your Godot installation is correctly set up.

Kotlin Script Integration: Try to write a simple Kotlin class, for instance, a character or an item, and integrate it into your Godot scene. Test if Godot correctly interacts with your Kotlin script.

API Connection Test in Game Context: Implement the OpenAI API call within a Godot project context. For example, create an NPC character that, when interacted with, sends a request to GPT-3.5 to generate a greeting and displays it.

3. Version Control
   Initialize a Git Repository: For your project and push it to GitHub or GitLab. Ensure you commit changes regularly to track your progress and secure your work.
4. Keep Testing
   Cross-Platform Compatibility: Regularly test your game on both Windows 10 and Ubuntu to ensure that there are no platform-specific issues, especially with Kotlin integration and OpenAI API calls.
   By following these steps, you'll have a solid foundation to build your project. Remember, complex projects benefit from an iterative, step-by-step approach. Get the basics right, ensure everything works as expected, and then proceed to build and integrate more complex features.
