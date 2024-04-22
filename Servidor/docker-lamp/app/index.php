<?php

$hostname = "db";
$username = "admin";
$password = "test";
$db = "database";

$conn = mysqli_connect($hostname, $username, $password, $db);
if ($conn->connect_error) {
    die("Database connection failed: " . $conn->connect_error);
}

// Función para escapar caracteres especiales en una cadena (para evitar inyecciones SQL)
function cleanInput($input) {
    global $conn;
    return mysqli_real_escape_string($conn, htmlspecialchars(strip_tags(trim($input))));
}

// Función para generar un hash seguro de la contraseña
function hashPassword($password) {
    // Generar un salt aleatorio
    $salt = bin2hex(random_bytes(32));
    // Combinar la contraseña con el salt y generar el hash
    $hashedPassword = hash('sha256', $password . $salt);
    return array('hash' => $hashedPassword, 'salt' => $salt);
}

// Función para verificar si la contraseña coincide con el hash almacenado
function verifyPassword($password, $storedHash, $storedSalt) {
    // Generar el hash de la contraseña ingresada con el salt almacenado
    $hashedPassword = hash('sha256', $password . $storedSalt);
    // Comparar el hash generado con el hash almacenado
    return $hashedPassword === $storedHash;
}

// Función para registrar un nuevo usuario
function registerUser($username, $password) {
    global $conn;

    // Escapar caracteres especiales
    $username = cleanInput($username);

    // Verificar si el usuario ya existe en la base de datos
    $query = mysqli_query($conn, "SELECT * FROM usuarios WHERE nombre='$username'");
    if (mysqli_num_rows($query) > 0) {
        return "El usuario ya existe";
    }

    // Generar el hash de la contraseña y obtener el salt
    $passwordData = hashPassword($password);
    $hashedPassword = $passwordData['hash'];
    $salt = $passwordData['salt'];

    // Insertar nuevo usuario en la base de datos
    $insert = mysqli_query($conn, "INSERT INTO usuarios (nombre, hash, salt) VALUES ('$username', '$hashedPassword', '$salt')");
    if ($insert) {
        return "Registro exitoso";
    } else {
        return "Error al registrar usuario";
    }
}

// Función para iniciar sesión de usuario
function loginUser($username, $password, $fcm_token) {
    global $conn;

    // Escapar caracteres especiales
    $username = cleanInput($username);

    // Obtener el hash de la contraseña almacenada y el salt asociado al usuario
    $query = mysqli_query($conn, "SELECT hash, salt FROM usuarios WHERE nombre='$username'");
    if (mysqli_num_rows($query) == 1) {
        $row = mysqli_fetch_assoc($query);
        $storedHash = $row['hash'];
        $storedSalt = $row['salt'];

        // Verificar si la contraseña ingresada coincide con el hash almacenado
        if (verifyPassword($password, $storedHash, $storedSalt)) {
            // Actualizar el token FCM en la base de datos
            $updateQuery = mysqli_query($conn, "UPDATE usuarios SET fcm_token='$fcm_token' WHERE nombre='$username'");
            
            if ($updateQuery) {
                return "Inicio de sesión exitoso";
            } else {
                return "Error al actualizar el token FCM";
            }
        } else {
            return "Contraseña incorrecta";
        }
    } else {
        return "Usuario no encontrado";
    }
}

// Función para cerrar sesión de usuario
function logoutUser($username) {
    global $conn;

    // Escapar caracteres especiales
    $username = cleanInput($username);

    // Actualizar el token FCM a null en la base de datos
    $updateQuery = mysqli_query($conn, "UPDATE usuarios SET fcm_token=NULL WHERE nombre='$username'");

    if ($updateQuery) {
        return "Cierre de sesión exitoso";
    } else {
        return "Error al cerrar sesión";
    }
}

// Verificar el tipo de solicitud y llamar a la función correspondiente
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Leer y decodificar los datos JSON del cuerpo de la solicitud
    $data = json_decode(file_get_contents('php://input'), true);

    // Verificar si se recibieron los datos esperados
    if (isset($data['action']) && isset($data['username']) && isset($data['password'])) {
        // Obtener los valores de los datos
        $action = $data['action'];
        $username = cleanInput($data['username']);
        $password = cleanInput($data['password']);
        $fcm_token = cleanInput($data['fcm_token']);

        // Añadir logs para ver los valores de action, username y password
        error_log('Valor de action: ' . $action);
        error_log('Valor de username: ' . $username);
        error_log('Valor de password: ' . $password);
        error_log('Valor de fcm_token: ' . $fcm_token);

        // Ejecutar la acción correspondiente
        switch ($action) {
            case 'register':
                echo registerUser($username, $password);
                break;
            case 'login':
                echo loginUser($username, $password, $fcm_token);
                break;  
            default:
                echo "Acción no válida";
        }
    } else {
        // Si no hay contraseña puede ser logout
        if(isset($data['action']) && isset($data['username'])){
            $action = $data['action'];
            $username = cleanInput($data['username']);
            if ($action == 'logout'){
                echo logoutUser($username);
            }
            else{
                // Datos incompletos en la solicitud
                echo "Datos incompletos en la solicitud";
            }
            
        }
        else{
            // Datos incompletos en la solicitud
            echo "Datos incompletos en la solicitud";
        }
    }
} else {
    // Método de solicitud no permitido
    echo "Método de solicitud no permitido";
}

mysqli_close($conn);
?>
