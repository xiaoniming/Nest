package com.example.ni.nest.Http.javabeans;

/**
 * Created by ni on 2016/8/4.
 */

public class UserCommentResponse {
    private Integer userId;
    private Boolean get;

    @Override
    public String toString() {
        return "userId: " + userId + " get: " + get;
    }
}

