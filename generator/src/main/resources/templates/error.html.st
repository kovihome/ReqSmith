<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Error</title>
</head>
<body>
<h1>Oops! Something went wrong.</h1>
<p>An error occurred while processing your request.</p>
<p th:text="'Status: ' + ${status}"></p>
<p th:text="'Message: ' + ${message}"></p>
<p th:text="'Timestamp: ' + ${timestamp}"></p>
</body>
</html>
