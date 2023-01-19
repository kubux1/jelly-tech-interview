# jelly-tech-interview

Requirements: [Marcura Java Developer Test.pdf](https://github.com/kubux1/jelly-tech-interview/files/10459301/Marcura.Java.Developer.Test.pdf)
<br /> <br />
To run it locally, you need Docker, Maven and access-key for https://fixer.io/. Once you have access-key insert it into **dokcer-compose.yaml** file by replacing **FIXER_API_ACCESS_KEY** placeholder with your key.

<br />
Then run following commands from the root of the project: <br />
1. mvn clean install <br />
2. docker compose up <br /><br />

Once all ready you should be able to run example GET request http://localhost:8080/exchange?from=EUR&to=PLN

API docs is available at http://localhost:8080/swagger-ui/index.html
