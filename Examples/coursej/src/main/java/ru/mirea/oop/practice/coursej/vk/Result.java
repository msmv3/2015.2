package ru.mirea.oop.practice.coursej.vk;

import com.google.gson.annotations.SerializedName;
import retrofit.Call;
import retrofit.Response;
import ru.mirea.oop.practice.coursej.vk.entities.Error;

import java.io.IOException;

public final class Result<E> {
    @SerializedName("response")
    public E response;
    @SuppressWarnings("unused")
    @SerializedName("error")
    public ru.mirea.oop.practice.coursej.vk.entities.Error error;

    public static <E> E call(Call<Result<E>> callable) throws IOException {
        if (callable == null)
            return null;
        Response<Result<E>> result = callable.execute();
        if (result.isSuccess() && result.body().response != null)
            return result.body().response;
        Error error = result.body().error;
        if (error != null) {
            System.out.println("Error: " + error.errorCode + ", Message: " + error.errorMessage);
            throw new RuntimeException(error.errorMessage);
        }
        System.out.println(result.errorBody());
        return null;
    }
}
