FROM ubuntu:20.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    openjdk-11-jdk \
    android-tools-adb \
    android-tools-fastboot \
    usbutils \
    libglu1-mesa \
    libnss3-dev \
    libgconf-2-4 \
    libxi6 \
    libgconf-2-4 \
    libxtst6 \
    libxrandr2 \
    libasound2-dev \
    libpangocairo-1.0-0 \
    libatk1.0-0 \
    libcairo-gobject2 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN curl -sL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV ANDROID_HOME=/opt/android
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools

# Create Android SDK directory
RUN mkdir -p $ANDROID_HOME

# Download and install Android SDK
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip -O sdk-tools.zip \
    && unzip sdk-tools.zip -d $ANDROID_HOME \
    && rm sdk-tools.zip \
    && mv $ANDROID_HOME/cmdline-tools $ANDROID_HOME/latest \
    && mkdir -p $ANDROID_HOME/cmdline-tools \
    && mv $ANDROID_HOME/latest $ANDROID_HOME/cmdline-tools/

ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Accept Android licenses and install necessary packages
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" \
    && sdkmanager "platforms;android-30" \
    && sdkmanager "build-tools;30.0.3" \
    && sdkmanager "system-images;android-30;google_apis;x86_64"

# Install Appium
RUN npm install -g appium@2.0.0 \
    && appium driver install uiautomator2 \
    && appium driver install xcuitest

# Install Appium Inspector (for debugging)
RUN npm install -g @appium/inspector

# Create appium user
RUN useradd -m -s /bin/bash appium \
    && usermod -aG plugdev appium

# Create log directory
RUN mkdir -p /var/log/appium \
    && chown appium:appium /var/log/appium

# Switch to appium user
USER appium
WORKDIR /home/appium

# Expose Appium port
EXPOSE 4723

# Start Appium server
CMD ["appium", "--address", "0.0.0.0", "--port", "4723", "--log-level", "info", "--log", "/var/log/appium/appium.log"]