<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class UpdateExpenseRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'amount' => ['sometimes', 'integer', 'min:1'],
            'category' => ['sometimes', 'string', 'max:50'],
            'note' => ['nullable', 'string', 'max:255'],
            'spent_at' => ['sometimes', 'date'],
        ];
    }
}
