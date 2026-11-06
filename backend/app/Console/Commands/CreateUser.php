<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;

class CreateUser extends Command
{
    protected $signature = 'costa:user {name} {email} {password}';

    protected $description = 'Create a Costa user or update its name and password';

    public function handle(): int
    {
        $user = User::query()->updateOrCreate(
            ['email' => $this->argument('email')],
            [
                'name' => $this->argument('name'),
                'password' => $this->argument('password'),
            ],
        );

        $this->info("User {$user->email} siap (id {$user->id}).");

        return self::SUCCESS;
    }
}
