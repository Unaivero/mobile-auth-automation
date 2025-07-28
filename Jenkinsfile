pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-11'
            args '-v /var/jenkins_home/.m2:/root/.m2'
        }
    }

    parameters {
        choice(name: 'PLATFORM', choices: ['android', 'ios', 'both'], description: 'Which platform to test on')
        string(name: 'TEST_GROUPS', defaultValue: 'all', description: 'Test groups to run (e.g. api, ui, security, all)')
        booleanParam(name: 'RUN_SECURITY_SCAN', defaultValue: false, description: 'Run OWASP ZAP security scan')
        booleanParam(name: 'DISTRIBUTE_REPORTS', defaultValue: true, description: 'Distribute test reports')
    }

    environment {
        ALLURE_VERSION = '2.20.1'
        API_BASE_URL = credentials('API_BASE_URL')
        TEST_API_KEY = credentials('TEST_API_KEY')
        TEST_USERNAME = credentials('TEST_USERNAME')
        TEST_PASSWORD = credentials('TEST_PASSWORD')
        ZAP_API_KEY = credentials('ZAP_API_KEY')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def changedFiles = sh(script: 'git diff --name-only HEAD~1 HEAD', returnStdout: true).trim()
                    
                    env.RUN_ALL_TESTS = changedFiles.contains('pom.xml') || changedFiles.contains('Jenkinsfile')
                    env.RUN_UI_TESTS = env.RUN_ALL_TESTS || changedFiles.contains('src/main/java/com/securitytests/pages/') || changedFiles.contains('src/test/java/com/securitytests/ui/')
                    env.RUN_API_TESTS = env.RUN_ALL_TESTS || changedFiles.contains('src/main/java/com/securitytests/api/') || changedFiles.contains('src/test/java/com/securitytests/api/')
                    env.RUN_SECURITY_TESTS = env.RUN_ALL_TESTS || params.RUN_SECURITY_SCAN || changedFiles.contains('src/main/java/com/securitytests/utils/security/') || changedFiles.contains('src/test/java/com/securitytests/security/')
                    
                    echo "Run all tests: ${env.RUN_ALL_TESTS}"
                    echo "Run UI tests: ${env.RUN_UI_TESTS}"
                    echo "Run API tests: ${env.RUN_API_TESTS}"
                    echo "Run security tests: ${env.RUN_SECURITY_TESTS}"
                }
            }
        }

        stage('Test Splitting') {
            steps {
                script {
                    def testClasses = sh(script: 'find src/test/java -name "*Test.java" | sort', returnStdout: true).trim().split("\n")
                    def totalTests = testClasses.size()
                    def nodeCount = sh(script: 'echo $NODE_TOTAL', returnStdout: true).trim() ?: '1'
                    def nodeIndex = sh(script: 'echo $NODE_INDEX', returnStdout: true).trim() ?: '0'
                    
                    nodeCount = nodeCount.toInteger()
                    nodeIndex = nodeIndex.toInteger()
                    
                    // Calculate test slice for this node
                    def testsPerNode = Math.ceil(totalTests / nodeCount)
                    def startIndex = nodeIndex * testsPerNode
                    def endIndex = Math.min((nodeIndex + 1) * testsPerNode, totalTests) - 1
                    
                    if (startIndex >= totalTests) {
                        echo "No tests to run on this node"
                        env.SKIP_TESTS = 'true'
                    } else {
                        def testSubset = []
                        for (int i = startIndex; i <= endIndex; i++) {
                            testSubset.add(testClasses[i])
                        }
                        
                        env.TEST_CLASSES = testSubset.collect { 
                            it.replace('src/test/java/', '')
                              .replace('/', '.')
                              .replace('.java', '')
                        }.join(',')
                        
                        echo "Tests to run on this node: ${env.TEST_CLASSES}"
                    }
                }
            }
        }

        stage('Run API Tests') {
            when {
                expression { return env.RUN_API_TESTS == 'true' && (params.TEST_GROUPS == 'all' || params.TEST_GROUPS == 'api') }
            }
            steps {
                sh '''
                    if [ "${SKIP_TESTS}" != "true" ]; then
                        if [ -n "${TEST_CLASSES}" ]; then
                            mvn clean test -DskipUiTests=true -DskipSecurityTests=true -Dgroups=api -Dtest=${TEST_CLASSES}
                        else
                            mvn clean test -DskipUiTests=true -DskipSecurityTests=true -Dgroups=api
                        fi
                    fi
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Setup Mobile Testing Environment') {
            when {
                expression { return env.RUN_UI_TESTS == 'true' && (params.TEST_GROUPS == 'all' || params.TEST_GROUPS == 'ui') }
            }
            steps {
                sh '''
                    # Install Node.js and Appium
                    curl -sL https://deb.nodesource.com/setup_16.x | bash -
                    apt-get install -y nodejs
                    npm install -g appium
                    appium driver install uiautomator2
                    appium driver install xcuitest
                    
                    # Start Appium Server
                    appium &
                    sleep 10
                    
                    # Install Android SDK if testing Android
                    if [ "${PLATFORM}" = "android" ] || [ "${PLATFORM}" = "both" ]; then
                        apt-get update
                        apt-get install -y wget unzip
                        wget https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip
                        unzip commandlinetools-linux-7583922_latest.zip -d /opt/android-sdk
                        export ANDROID_HOME=/opt/android-sdk
                        export PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin:$ANDROID_HOME/platform-tools
                        yes | sdkmanager --licenses
                        sdkmanager "platform-tools" "platforms;android-30" "system-images;android-30;google_apis;x86_64"
                        echo "Android SDK installed"
                    fi
                '''
            }
        }

        stage('Run UI Tests') {
            when {
                expression { return env.RUN_UI_TESTS == 'true' && (params.TEST_GROUPS == 'all' || params.TEST_GROUPS == 'ui') }
            }
            parallel {
                stage('Android Tests') {
                    when {
                        expression { return params.PLATFORM == 'android' || params.PLATFORM == 'both' }
                    }
                    steps {
                        sh '''
                            if [ "${SKIP_TESTS}" != "true" ]; then
                                if [ -n "${TEST_CLASSES}" ]; then
                                    mvn test -DskipApiTests=true -DskipSecurityTests=true -Dgroups=ui -Dplatform=android -Dtest=${TEST_CLASSES}
                                else
                                    mvn test -DskipApiTests=true -DskipSecurityTests=true -Dgroups=ui -Dplatform=android
                                fi
                            fi
                        '''
                    }
                }
                stage('iOS Tests') {
                    when {
                        expression { return params.PLATFORM == 'ios' || params.PLATFORM == 'both' }
                    }
                    steps {
                        echo "iOS tests would run on a macOS agent. Skipped in this pipeline."
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Setup ZAP') {
            when {
                expression { return env.RUN_SECURITY_TESTS == 'true' && (params.TEST_GROUPS == 'all' || params.TEST_GROUPS == 'security') }
            }
            steps {
                sh '''
                    # Pull and start ZAP container
                    docker pull owasp/zap2docker-stable
                    docker run -d --name zap -p 8080:8080 -p 8090:8090 -i owasp/zap2docker-stable zap-x.sh -daemon -host 0.0.0.0 -port 8080 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true -config api.key=${ZAP_API_KEY}
                    sleep 30
                '''
            }
        }

        stage('Run Security Tests') {
            when {
                expression { return env.RUN_SECURITY_TESTS == 'true' && (params.TEST_GROUPS == 'all' || params.TEST_GROUPS == 'security') }
            }
            steps {
                sh '''
                    if [ "${SKIP_TESTS}" != "true" ]; then
                        if [ -n "${TEST_CLASSES}" ]; then
                            mvn test -DskipUiTests=true -Dgroups=security -Dzap.host=localhost -Dzap.port=8080 -Dzap.apiKey=${ZAP_API_KEY} -Dtest=${TEST_CLASSES}
                        else
                            mvn test -DskipUiTests=true -Dgroups=security -Dzap.host=localhost -Dzap.port=8080 -Dzap.apiKey=${ZAP_API_KEY}
                        fi
                    fi
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/zap-security-report.html', allowEmptyArchive: true
                }
            }
        }

        stage('Generate Reports') {
            when {
                expression { return params.DISTRIBUTE_REPORTS }
            }
            steps {
                sh '''
                    wget https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.zip
                    unzip allure-${ALLURE_VERSION}.zip
                    ./allure-${ALLURE_VERSION}/bin/allure generate target/allure-results -o target/allure-report
                '''
                
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/allure-report',
                    reportFiles: 'index.html',
                    reportName: 'Allure Test Report'
                ])
            }
        }

        stage('Send Notifications') {
            when {
                expression { return params.DISTRIBUTE_REPORTS }
            }
            steps {
                sh '''
                    mvn exec:java -Dexec.mainClass="com.securitytests.utils.notification.NotificationSender" -Dexec.args="--subject 'Test Execution Results' --body 'Test execution on Jenkins completed. See results at ${BUILD_URL}' --priority HIGH"
                '''
                
                emailext (
                    subject: "Test Execution Results - ${currentBuild.fullDisplayName}",
                    body: """<p>Test execution on Jenkins completed.</p>
                    <p>Build URL: <a href="${BUILD_URL}">${BUILD_URL}</a></p>
                    <p>Test Report: <a href="${BUILD_URL}Allure_20Test_20Report/">${BUILD_URL}Allure_20Test_20Report/</a></p>""",
                    to: '${NOTIFICATION_EMAIL_RECIPIENTS}',
                    mimeType: 'text/html'
                )
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
