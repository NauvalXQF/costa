<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Str;

class IssueApiToken extends Command
{
    protected $signature = 'costa:issue-token {email}';

    protected $description = 'Create the Costa user if needed and issue a Sanctum API token';

    public function handle(): int
    {
        $user = User::query()->firstOrCreate(
            ['email' => $this->argument('email')],
            ['name' => 'Nauval', 'password' => Str::random(32)]
        );

        $token = $user->createToken('costa-api')->plainTextToken;

        $this->line($token);

        return self::SUCCESS;
    }
}
