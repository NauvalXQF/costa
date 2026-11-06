<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Setting extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'key',
        'value',
    ];

    protected function casts(): array
    {
        return [
            'value' => 'json',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public static function getFor(int $userId, string $key, mixed $default = null): mixed
    {
        $setting = static::query()
            ->where('user_id', $userId)
            ->where('key', $key)
            ->first();

        return $setting?->value ?? $default;
    }

    public static function putFor(int $userId, string $key, mixed $value): void
    {
        static::query()->updateOrCreate(
            ['user_id' => $userId, 'key' => $key],
            ['value' => $value],
        );
    }
}
