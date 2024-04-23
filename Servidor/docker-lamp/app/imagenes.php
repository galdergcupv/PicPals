<?php

$hostname = "db";
$username = "admin";
$password = "test";
$db = "database";

$conn = mysqli_connect($hostname, $username, $password, $db);
if ($conn->connect_error) {
    die("Database connection failed: " . $conn->connect_error);
}

// Función para escapar caracteres especiales (para evitar inyecciones SQL)
function cleanInput($input) {
    global $conn;
    return mysqli_real_escape_string($conn, htmlspecialchars(strip_tags(trim($input))));
}

// Función para guardar una imagen en la base de datos
function saveImage($filename, $uploaderName, $imageBase64, $shareWith) {
    global $conn;

    // Escapar caracteres especiales
    $filename = cleanInput($filename);
    $uploaderName = cleanInput($uploaderName);
    $imageData = $imageBase64;

    // Insertar la imagen en la base de datos
    $insert = mysqli_query($conn, "INSERT INTO imagenes (filename, uploader_name, imagen) VALUES ('$filename', '$uploaderName', '$imageData')");
    if ($insert) {
        $imageId = mysqli_insert_id($conn);

        // Verificar si se ha proporcionado usuarios para compartir
        if (!empty($shareWith)) {
            // Dividir los usuarios por comas
            $usersToShare = explode(",", $shareWith);

            // Iterar sobre los usuarios y agregar entradas en la tabla image_sharing
            foreach ($usersToShare as $user) {
                $user = trim($user);
    
                // Comprobar si existe el usuario
                $userQuery = mysqli_query($conn, "SELECT nombre, fcm_token FROM usuarios WHERE nombre = '$user'");
                if (mysqli_num_rows($userQuery) > 0) {
                    $userData = mysqli_fetch_assoc($userQuery);
                    $fcmToken = $userData['fcm_token'];
                    
                    // Enviar notificaciones fcm al usuario
                    $message = "$uploaderName ha compartido una foto contigo!";
                    sendNotification($fcmToken, $message);
    
                    // Insertar la entrada en la tabla image_sharing
                    mysqli_query($conn, "INSERT INTO image_sharing (image_id, username) VALUES ($imageId, '$user')");
                }
            }
        }

        return "Imagen guardada con éxito";
    } else {
        error_log(mysqli_error($conn));
        return "Error al guardar la imagen";
    }
}



// Función para obtener todas las imágenes subidas por un usuario
function getUserImages($username) {
    global $conn;

    // Escapar caracteres especiales
    $username = cleanInput($username);

    // Obtener todas las imágenes subidas por el usuario
    $query = mysqli_query($conn, "SELECT filename, imagen, uploader_name FROM imagenes WHERE uploader_name='$username'");
    $images = array();
    if (mysqli_num_rows($query) > 0) {
        while ($row = mysqli_fetch_assoc($query)) {
            $images[] = $row;
        }
        return $images;
    } else {
        return "No se encontraron imagenes para este usuario";
    }
}

// Función para obtener todas las imágenes compartidas con un usuario
function getSharedImages($username) {
    global $conn;

    // Escapar caracteres especiales
    $username = cleanInput($username);

    // Obtener todas las imágenes compartidas con el usuario
    $query = mysqli_query($conn, "SELECT i.filename, i.imagen, i.uploader_name FROM imagenes i INNER JOIN image_sharing s ON i.id = s.image_id WHERE s.username='$username'");
    $images = array();
    if (mysqli_num_rows($query) > 0) {
        while ($row = mysqli_fetch_assoc($query)) {
            $images[] = $row;
        }
        return $images;
    } else {
        return "No se encontraron imagenes compartidas para este usuario";
    }
}

// Función para enviar una notificación fcm al usuario
function sendNotification($fcmToken, $message) {
    $url = 'https://fcm.googleapis.com/fcm/send';

    // Key del servidor fcm
    $serverKey = 'AAAAhYrLqXg:APA91bF9DSIUYzPVgf5Q01fVHK3MEedUEJ4FKk1_glDjI2NO389p6xwuwCbFZJz9iOzQN4KW-fj4quLugtWixsvU4nFlrY7xF2L7_ZYFed_DHR26W48MKqnxSMRCUl3_ztX0qBT06rm5';

    // Datos del mensaje
    $data = [
        'to' => $fcmToken,
        'notification' => [
            'title' => 'Nueva Imagen Compartida',
            'body' => $message
        ]
    ];

    // Cabeceras HTTP
    $headers = [
        'Authorization: key=' . $serverKey,
        'Content-Type: application/json'
    ];

    // cURL
    $curl = curl_init();

    curl_setopt($curl, CURLOPT_URL, $url);
    curl_setopt($curl, CURLOPT_POST, true);
    curl_setopt($curl, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($curl, CURLOPT_POSTFIELDS, json_encode($data));

    $response = curl_exec($curl);

    // Añadir logs para ver los datos
    error_log("FCM Request Data: " . json_encode($data));

    if ($response === false) {
        // Añadir logs para ver errores de curl
        error_log("FCM Error: " . curl_error($curl));
    } else {
        // Añadir logs para ver la respuesta
        error_log("FCM Response: " . $response);
    }

    curl_close($curl);

    return $response;
}



// Verificar el tipo de solicitud y llamar a la función correspondiente
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Leer y decodificar los datos JSON del cuerpo de la solicitud
    $data = json_decode(file_get_contents('php://input'), true);

    error_log('Valor de action: ' . $data['action']);
    error_log('Valor de filename: ' . $data['filename']);
    error_log('Valor de uploaderName: ' . $data['uploaderName']);
    error_log('Valor de imageBase64: ' . $data['imageBase64']);
    error_log('Valor de shareWith: ' . $data['shareWith']);

    // Verificar si se recibieron los datos esperados
    if (isset($data['action'])) {
        // Obtener el valor de la acción
        $action = $data['action'];

        // Ejecutar la acción correspondiente
        switch ($action) {
            case 'save_image':
                // Verificar si se recibieron todos los datos necesarios
                if (isset($data['filename']) && isset($data['uploaderName']) && isset($data['imageBase64'])) {
                    $filename = $data['filename'];
                    $uploaderName = $data['uploaderName'];
                    $imageBase64 = $data['imageBase64'];
                    $shareWith = isset($data['shareWith']) ? $data['shareWith'] : "";
                    echo saveImage($filename, $uploaderName, $imageBase64, $shareWith); 
                } else {
                    echo "Datos incompletos para guardar la imagen";
                }
                break;
            case 'get_user_images':
                if (isset($data['username'])) { 
                    $username = $data['username']; 
                    echo json_encode(getUserImages($username)); 
                } else {
                    echo "Nombre de usuario no especificado"; 
                }
                break;
            
            case 'get_shared_images':
                if (isset($data['username'])) { 
                $username = $data['username']; 
                echo json_encode(getSharedImages($username)); 
        } else {
            echo "Nombre de usuario no especificado"; 
        }
        break;
            default:
                echo "Acción no válida";
        }
    } else {
        echo "Acción no especificada";
    }
} else {
    echo "Método de solicitud no permitido";
}

mysqli_close($conn);
?>
