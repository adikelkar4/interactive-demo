/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuodb.storefront.exception.StorefrontException;
import com.nuodb.storefront.model.dto.Message;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.ParamException;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ExceptionProvider implements ExceptionMapper<RuntimeException> {
    public ExceptionProvider() {
    }

    @Override
    public Response toResponse(RuntimeException exception) {
        Status errorCode;

        if (exception instanceof StorefrontException) {
            errorCode = ((StorefrontException) exception).getErrorCode();
        } else if (exception instanceof IllegalArgumentException || exception instanceof ParamException)  {
            errorCode = Status.BAD_REQUEST;
        } else if (exception instanceof NotFoundException) {
            errorCode = Status.NOT_FOUND;
        } else {
            errorCode = Status.INTERNAL_SERVER_ERROR;
        }

        String jsonString = "";
		try {
			jsonString = new ObjectMapper().writeValueAsString(new Message(exception));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(errorCode).entity(jsonString).build();
    }
}
