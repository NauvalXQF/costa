<?php

use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return response()->json([
        'app' => 'Costa API',
        'status' => 'ok',
    ]);
});

Route::get('/download/costa.apk', function () {
    $path = storage_path('app/public/costa.apk');

    abort_unless(file_exists($path), 404, 'APK belum tersedia.');

    return response()->download($path, 'costa.apk', [
        'Content-Type' => 'application/vnd.android.package-archive',
    ]);
});
