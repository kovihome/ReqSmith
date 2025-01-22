<!--
  ~ ReqSmith - Build application from requirements
  ~ Copyright (c) 2025. Kovi <kovihome86@gmail.com>
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

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
