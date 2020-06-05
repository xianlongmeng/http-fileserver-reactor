<!DOCTYPE html>
<html lang="en">
<head>
    <style>
        table td{
            vertical-align:top;
            border:solid 1px #888;
            padding:10px;
        }
    </style>
</head>
<body>
<h1>My FreeMarker 400 Error Page</h1>
<table>
    <tr>
        <td>Date</td>
        <td>${timestamp?datetime}</td>
    </tr>
    <tr>
        <td>Error</td>
        <td>${error}</td>
    </tr>
    <tr>
        <td>Status</td>
        <td>${status}</td>
    </tr>
    <tr>
        <td>Message</td>
        <td>${message!"400"}</td>
    </tr>
    <tr>
        <td>Exception</td>
        <td>${exception!"No exception thrown"}</td>
    </tr>
    <tr>
        <td>Trace</td>
        <td>
            <pre>${trace!"No Stacktrace available"}</pre>
        </td>
    </tr>
</table>
</body>
</html>