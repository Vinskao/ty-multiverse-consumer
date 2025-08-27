pipeline {
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  serviceAccountName: jenkins-admin
                  imagePullSecrets:
                  - name: dockerhub-credentials
                  containers:
                  - name: maven
                    image: maven:3.9.8-eclipse-temurin-21
                    command: ["cat"]
                    tty: true
                    resources:
                      requests:
                        cpu: "100m"
                        memory: "1024Mi"
                      limits:
                        cpu: "500m"
                        memory: "2048Mi"
                    volumeMounts:
                    - mountPath: /root/.m2
                      name: maven-repo
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                    workingDir: /home/jenkins/agent
                  - name: docker
                    image: docker:23-dind
                    privileged: true
                    securityContext:
                      privileged: true
                    resources:
                      requests:
                        cpu: "50m"
                        memory: "512Mi"
                      limits:
                        cpu: "200m"
                        memory: "1024Mi"
                    env:
                    - name: DOCKER_TLS_CERTDIR
                      value: ""
                    - name: DOCKER_BUILDKIT
                      value: "1"
                    volumeMounts:
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                  - name: kubectl
                    image: bitnami/kubectl:1.30.7
                    command: ["/bin/sh"]
                    args: ["-c", "while true; do sleep 30; done"]
                    imagePullPolicy: Always
                    securityContext:
                      runAsUser: 0
                    resources:
                      requests:
                        cpu: "25m"
                        memory: "256Mi"
                      limits:
                        cpu: "100m"
                        memory: "512Mi"
                    volumeMounts:
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                  volumes:
                  - name: maven-repo
                    emptyDir: {}
                  - name: workspace-volume
                    emptyDir: {}
            '''
            defaultContainer 'maven'
            inheritFrom 'default'
        }
    }
    options {
        timestamps()
        disableConcurrentBuilds()
    }
    environment {
        DOCKER_IMAGE = 'papakao/ty-multiverse-consumer'
        DOCKER_TAG = "${BUILD_NUMBER}"
        SERVER_PORT = "8081"
        LOGGING_LEVEL = "INFO"
        LOGGING_LEVEL_SPRINGFRAMEWORK = "INFO"
    }
    stages {
        stage('Clone and Setup') {
            steps {
                script {
                    container('maven') {
                        sh '''
                            # 確認 Dockerfile 存在
                            ls -la
                            if [ ! -f "Dockerfile" ]; then
                                echo "Error: Dockerfile not found!"
                                exit 1
                            fi
                            # 創建配置目錄
                            mkdir -p src/main/resources/env
                        '''
                        withCredentials([
                            string(credentialsId: 'SPRING_DATASOURCE_URL', variable: 'SPRING_DATASOURCE_URL'),
                            string(credentialsId: 'SPRING_DATASOURCE_USERNAME', variable: 'SPRING_DATASOURCE_USERNAME'),
                            string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD'),
                            string(credentialsId: 'SPRING_DATASOURCE_URL', variable: 'SPRING_DATASOURCE_URL'),
                            string(credentialsId: 'SPRING_DATASOURCE_USERNAME', variable: 'SPRING_DATASOURCE_USERNAME'),
                            string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD'),
                            string(credentialsId: 'REDIS_HOST', variable: 'REDIS_HOST'),
                            string(credentialsId: 'REDIS_CUSTOM_PORT', variable: 'REDIS_CUSTOM_PORT'),
                            string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD'),
                            string(credentialsId: 'REDIS_QUEUE_TYMB', variable: 'REDIS_QUEUE_TYMB'),
                            string(credentialsId: 'PUBLIC_TYMB_URL', variable: 'PUBLIC_TYMB_URL'),
                            string(credentialsId: 'PUBLIC_FRONTEND_URL', variable: 'PUBLIC_FRONTEND_URL'),
                            string(credentialsId: 'KEYCLOAK_AUTH_SERVER_URL', variable: 'KEYCLOAK_AUTH_SERVER_URL'),
                            string(credentialsId: 'PUBLIC_REALM', variable: 'PUBLIC_REALM'),
                            string(credentialsId: 'PUBLIC_CLIENT_ID', variable: 'PUBLIC_CLIENT_ID'),
                            string(credentialsId: 'KEYCLOAK_CREDENTIALS_SECRET', variable: 'KEYCLOAK_CREDENTIALS_SECRET')
                        ]) {
                            sh '''
                                cat > src/main/resources/env/platform.properties <<EOL
                                env=platform
                                spring.profiles.active=platform
                                PROJECT_ENV=platform
                                # Primary datasource configuration
                                SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
                                SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
                                SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
                                # People datasource configuration
                                SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
                                SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
                                SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
                                # People datasource tokens for resource filtering
                                PEOPLE_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
                                PEOPLE_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
                                PEOPLE_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
                                server.port=8081
                                logging.level.root=INFO
                                logging.level.org.springframework=INFO
                                PUBLIC_TYMB_URL=${PUBLIC_TYMB_URL}
                                PUBLIC_FRONTEND_URL=${PUBLIC_FRONTEND_URL}
                                KEYCLOAK_AUTH_SERVER_URL=${KEYCLOAK_AUTH_SERVER_URL}
                                PUBLIC_REALM=${PUBLIC_REALM}
                                PUBLIC_CLIENT_ID=${PUBLIC_CLIENT_ID}
                                KEYCLOAK_CREDENTIALS_SECRET=${KEYCLOAK_CREDENTIALS_SECRET}
                                REDIS_HOST=${REDIS_HOST}
                                REDIS_CUSTOM_PORT=${REDIS_CUSTOM_PORT}
                                REDIS_PASSWORD=${REDIS_PASSWORD}
                                REDIS_QUEUE_TYMB=${REDIS_QUEUE_TYMB}
                                # 明确禁用people-datasource
                                SPRING_DATASOURCE_ENABLED=false
                                # RabbitMQ 配置 - Production 環境啟用
                                RABBITMQ_ENABLED=true
                                RABBITMQ_HOST=rabbitmq-service
                                RABBITMQ_PORT=5672
                                RABBITMQ_USERNAME=admin
                                RABBITMQ_PASSWORD=admin123
                                RABBITMQ_VIRTUAL_HOST=/
                                EOL
                            '''
                        }
                    }
                }
            }
        }

        stage('Build') {
            steps {
                container('maven') {
                    sh 'MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC" mvn -T 1C -Dmaven.javadoc.skip=true clean package -P platform -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                container('maven') {
                    sh 'MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC" mvn -T 1C -Dmaven.javadoc.skip=true test -P platform'
                }
            }
        }

        stage('Build Docker Image with BuildKit') {
            steps {
                container('docker') {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                            sh '''
                                cd "${WORKSPACE}"
                                
                                # Docker login with retry mechanism
                                echo "Attempting Docker login..."
                                for i in {1..3}; do
                                    echo "Docker login attempt $i/3"
                                    if echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin; then
                                        echo "Docker login successful"
                                        break
                                    else
                                        echo "Docker login attempt $i failed"
                                        if [ $i -eq 3 ]; then
                                            echo "All Docker login attempts failed"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                                
                                # 確認 Dockerfile 存在
                                ls -la
                                if [ ! -f "Dockerfile" ]; then
                                    echo "Error: Dockerfile not found!"
                                    exit 1
                                fi
                                
                                # 構建 Docker 鏡像（啟用 BuildKit 與多平台參數）
                                echo "Building Docker image..."
                                docker build \
                                    --build-arg BUILDKIT_INLINE_CACHE=1 \
                                    --cache-from ${DOCKER_IMAGE}:latest \
                                    -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                                    -t ${DOCKER_IMAGE}:latest \
                                    .
                                    
                                # Push with retry mechanism
                                echo "Pushing Docker images..."
                                for i in {1..3}; do
                                    echo "Push attempt $i/3 for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                    if docker push ${DOCKER_IMAGE}:${DOCKER_TAG}; then
                                        echo "Successfully pushed ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                        break
                                    else
                                        echo "Push attempt $i failed for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                        if [ $i -eq 3 ]; then
                                            echo "All push attempts failed for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                                
                                for i in {1..3}; do
                                    echo "Push attempt $i/3 for ${DOCKER_IMAGE}:latest"
                                    if docker push ${DOCKER_IMAGE}:latest; then
                                        echo "Successfully pushed ${DOCKER_IMAGE}:latest"
                                        break
                                    else
                                        echo "Push attempt $i failed for ${DOCKER_IMAGE}:latest"
                                        if [ $i -eq 3 ]; then
                                            echo "All push attempts failed for ${DOCKER_IMAGE}:latest"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                            '''
                        }
                    }
                }
            }
        }

        stage('Debug Environment') {
            steps {
                container('kubectl') {
                    script {
                        echo "=== Listing all environment variables ==="
                        sh 'printenv | sort'
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                container('kubectl') {
                    withCredentials([
                        string(credentialsId: 'SPRING_DATASOURCE_URL', variable: 'SPRING_DATASOURCE_URL'),
                        string(credentialsId: 'SPRING_DATASOURCE_USERNAME', variable: 'SPRING_DATASOURCE_USERNAME'),
                        string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD'),
                        string(credentialsId: 'SPRING_DATASOURCE_URL', variable: 'SPRING_DATASOURCE_URL'),
                        string(credentialsId: 'SPRING_DATASOURCE_USERNAME', variable: 'SPRING_DATASOURCE_USERNAME'),
                        string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD'),
                        string(credentialsId: 'REDIS_HOST', variable: 'REDIS_HOST'),
                        string(credentialsId: 'REDIS_CUSTOM_PORT', variable: 'REDIS_CUSTOM_PORT'),
                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD'),
                        string(credentialsId: 'REDIS_QUEUE_TYMB', variable: 'REDIS_QUEUE_TYMB'),
                        string(credentialsId: 'PUBLIC_TYMB_URL', variable: 'PUBLIC_TYMB_URL'),
                        string(credentialsId: 'PUBLIC_FRONTEND_URL', variable: 'PUBLIC_FRONTEND_URL'),
                        string(credentialsId: 'KEYCLOAK_AUTH_SERVER_URL', variable: 'KEYCLOAK_AUTH_SERVER_URL'),
                        string(credentialsId: 'PUBLIC_REALM', variable: 'PUBLIC_REALM'),
                        string(credentialsId: 'PUBLIC_CLIENT_ID', variable: 'PUBLIC_CLIENT_ID'),
                        string(credentialsId: 'KEYCLOAK_CREDENTIALS_SECRET', variable: 'KEYCLOAK_CREDENTIALS_SECRET')
                    ]) {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                            script {
                                try {
                                    sh '''
                                        set -e

                                        # Ensure envsubst is available (try Debian then Alpine)
                                        if ! command -v envsubst >/dev/null 2>&1; then
                                          (apt-get update && apt-get install -y --no-install-recommends gettext-base ca-certificates) >/dev/null 2>&1 || true
                                          command -v envsubst >/dev/null 2>&1 || (apk add --no-cache gettext ca-certificates >/dev/null 2>&1 || true)
                                        fi

                                        # In-cluster auth via ServiceAccount (serviceAccountName: jenkins-admin)
                                        kubectl cluster-info

                                        # Ensure Docker Hub imagePullSecret exists in default namespace
                                        kubectl create secret docker-registry dockerhub-credentials \
                                          --docker-server=https://index.docker.io/v1/ \
                                          --docker-username="${DOCKER_USERNAME}" \
                                          --docker-password="${DOCKER_PASSWORD}" \
                                          --docker-email="none" \
                                          -n default \
                                          --dry-run=client -o yaml | kubectl apply -f -

                                        # Inspect manifest directory
                                        ls -la k8s/

                                        echo "Recreating deployment ..."
                                        echo "=== Effective sensitive env values ==="
                                        echo "SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}"
                                        echo "SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}"
                                        echo "KEYCLOAK_AUTH_SERVER_URL=${KEYCLOAK_AUTH_SERVER_URL}"
                                        echo "REDIS_HOST=${REDIS_HOST}:${REDIS_CUSTOM_PORT}"

                                        kubectl delete deployment ty-multiverse-consumer -n default --ignore-not-found
                                        envsubst < k8s/deployment.yaml | kubectl apply -f -
                                        kubectl set image deployment/ty-multiverse-consumer ty-multiverse-consumer=${DOCKER_IMAGE}:${DOCKER_TAG} -n default
                                        kubectl rollout status deployment/ty-multiverse-consumer -n default
                                    '''

                                    // 檢查部署狀態
                                    sh 'kubectl get deployments -n default'
                                    sh 'kubectl rollout status deployment/ty-multiverse-consumer -n default'
                                } catch (Exception e) {
                                    echo "Error during deployment: ${e.message}"
                                    // Debug non-ready pods and recent events
                                    sh '''
                                        set +e
                                        echo "=== Debug: pods for ty-multiverse-consumer ==="
                                        kubectl get pods -n default -l app.kubernetes.io/name=ty-multiverse-consumer -o wide || true

                                        echo "=== Debug: describe non-ready pods ==="
                                        for p in $(kubectl get pods -n default -l app.kubernetes.io/name=ty-multiverse-consumer -o jsonpath='{.items[?(@.status.conditions[?(@.type=="Ready")].status!="True")].metadata.name}'); do
                                          echo "--- $p"
                                          kubectl describe pod -n default "$p" || true
                                          echo "=== Last 200 logs for $p ==="
                                          kubectl logs -n default "$p" --tail=200 || true
                                        done

                                        echo "=== Recent events (default ns) ==="
                                        kubectl get events -n default --sort-by=.lastTimestamp | tail -n 100 || true
                                    '''
                                    throw e
                                }
                            } // end script
                        } // end inner withCredentials
                    } // end outer withCredentials
                } // end container
            } // end steps
        } // end stage
    }
    post {
        always {
            script {
                if (env.WORKSPACE) {
                    cleanWs()
                }
            }
        }
    }
}