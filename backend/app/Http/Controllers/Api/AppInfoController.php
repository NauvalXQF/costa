<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;

class AppInfoController extends Controller
{
    public function version()
    {
        $defaults = [
            'version_code' => 1,
            'version_name' => '1.0',
            'apk_url' => url('/download/costa.apk'),
            'notes' => '',
        ];

        $path = storage_path('app/app-version.json');

        if (! is_file($path)) {
            return response()->json($defaults);
        }

        $data = json_decode((string) file_get_contents($path), true);

        return response()->json(array_merge($defaults, is_array($data) ? $data : []));
    }
}
