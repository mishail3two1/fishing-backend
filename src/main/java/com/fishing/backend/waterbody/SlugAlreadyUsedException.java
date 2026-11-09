package com.fishing.backend.waterbody;

// slug уже занят другим водоёмом; в HTTP 409 его переводит GlobalExceptionHandler.
public class SlugAlreadyUsedException extends RuntimeException {
}
