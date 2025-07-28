FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    openjdk-11-jdk \
    android-tools-adb \
    xvfb \
    x11vnc \
    fluxbox \
    pulseaudio \
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
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV ANDROID_HOME=/opt/android
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

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
    && sdkmanager "emulator" \
    && sdkmanager "system-images;android-30;google_apis;x86_64"

# Create AVD
RUN echo "no" | avdmanager create avd -n test_emulator -k "system-images;android-30;google_apis;x86_64" -c 2048M --force

# Create startup script
RUN echo '#!/bin/bash\n\
export DISPLAY=:99\n\
Xvfb :99 -screen 0 1024x768x24 -ac +extension GLX +render -noreset &\n\
fluxbox &\n\
sleep 5\n\
cd $ANDROID_HOME\n\
adb start-server\n\
emulator -avd test_emulator -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim -camera-back none -camera-front none -qemu -enable-kvm &\n\
adb wait-for-device shell "while [[ -z \\$(getprop sys.boot_completed) ]]; do sleep 1; done;"\n\
echo "Emulator started successfully"\n\
tail -f /dev/null' > /start_emulator.sh

RUN chmod +x /start_emulator.sh

# Expose ADB and emulator ports
EXPOSE 5554 5555 5037

CMD ["/start_emulator.sh"]