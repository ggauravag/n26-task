# Live Transaction Statistics in O(1)

This application is designed to store transaction related data, and serve aggregated statistics in constant time and space complexity for transactions within last 60 seconds

## How to run it?

Do ```mvn package spring-boot:repackage``` and run ```java -jar target/task-1.0-SNAPSHOT.jar```

## Run Unit Tests
```mvn test```

## Run Integration Tests
```mvn verify```

## Improvements to be done

- Better error handling in case there are no transactions for last 60 seconds, instead of 404 status code as per current handling