<!DOCTYPE html>
<html>
<head>
    <title>Image Uploader</title>
</head>
<body>

<h2>Upload Image</h2>
<form method="post" action="">
    <label for="uploaderName">Uploader Name:</label><br>
    <input type="text" id="uploaderName" name="uploaderName" value="galder3"><br>
    <label for="shareWith">Share With:</label><br>
    <input type="text" id="shareWith" name="shareWith" value="galder2"><br>
    <label for="filename">Filename:</label><br>
    <input type="text" id="filename" name="filename" value="desde fuera"><br>
    <label for="imageBase64">Image Base64:</label><br>
    <textarea id="imageBase64" name="imageBase64" rows="6" cols="50">iVBORw0KGgoAAAANSUhEUgAAABIAAAAZCAIAAACza+nDAAAAA3NCSVQICAjb4U\/gAAABc0lEQVQ4\njWMUEBBgIB0wkaFnVBs2wILGF+bhePvlh5K0sJe1HiMTy39Ovr3HL12\/cRtNGSM83lhYGJ10Zbm5\nOPj5eQX4+VbvPpmbEMjNy8fBxcnMzPLv37+f378b2XpYOvii2GalLsnBzfv1+\/ef\/769fvdVgIt1\n+45DbGwsnOwswgI83NzsQsKi7Fy8KH7zsNJiZmbm5eNnZeeQk5fnZPnNycb++\/dvZibGV5\/\/fPzN\nJiajLKui\/u3rDxRtgkJCDAwMElKSMjIyq7fuY2Vl5eVhZ2Zm4eHlF+AX5OXlFxaT5BUQ+fvvN0qQ\nrNp21E5H5u\/f3z9+\/eTm5GDn4f\/+8puCjOTfn3+8PR05ObkFRfgZGJm\/f\/6MYtvff\/9+\/f4jKCS4\nbc+RaF87QRGJ7z9\/vv303dbJVlFNzcDanp2dk52D4927t+ghCQdmukpe9sb3nr7m42SNTMl6\/urt\nkcNHlixb++b9ZywRQBIYEmlyVBu1tAEANW5uFhp5crAAAAAASUVORK5CYII=\n</textarea><br><br>
    <input type="submit" value="Upload Image">
</form>

<div id="response">
    <?php
    if ($_SERVER["REQUEST_METHOD"] == "POST") {
        // Retrieve form data
        $uploaderName = $_POST['uploaderName'];
        $shareWith = $_POST['shareWith'];
        $filename = $_POST['filename'];
        $imageBase64 = $_POST['imageBase64'];

        // Define your JSON payload
        $data = array(
            "action" => "save_image",
            "filename" => $filename,
            "uploaderName" => $uploaderName,
            "shareWith" => $shareWith,
            "imageBase64" => $imageBase64
        );

        // Convert the data to JSON format
        $json_data = json_encode($data);

        // Set the URL for your server
        $url = 'http://34.29.139.252:81/imagenes.php';

        // Initialize cURL session
        $ch = curl_init();

        // Set cURL options
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $json_data);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

        // Execute the request
        $response = curl_exec($ch);

        // Check for errors
        if (curl_errno($ch)) {
            echo 'Error: ' . curl_error($ch);
        }

        // Close cURL session
        curl_close($ch);
    }
    ?>
</div>

</body>
</html>
