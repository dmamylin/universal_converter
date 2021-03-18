1. Building:
```mvn package```
2. Running:
```java -jar server/target/unit-converter-server-1.0-SNAPSHOT.jar units.csv```
3. Performing requests:
```curl -X POST 'localhost:80/convert' -H 'Content-Type: application/json' -d '{"from":"м", "to":"км"}'```

Task: https://github.com/gnkoshelev/universal-converter