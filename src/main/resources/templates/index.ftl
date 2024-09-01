<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Last photo</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 90vh;
            margin: 0;
            background-color: #f4f4f4;
        }
        img {
            width: 50%;
            height: auto;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }
        .device-info {
            text-align: center;
            font-weight: bold;
        }
        .info {
            display: flex;
            justify-content: space-between;
            width: 50%;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="device-info">
        <p>Device ID: ${deviceId}</p>
    </div>
    <img src="${url}" alt="Photo" />
    <div class="info">
        <p>Date: ${dateTime}</p>
        <p>Size: ${size}</p>
    </div>
</body>
</html>
