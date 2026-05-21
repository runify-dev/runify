package com.run.dagger.module;

import com.run.route.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  19:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class InitModel {
    @Inject
    @Singleton
    @Named("initializeApp")
    @Provides
    public String initializeApp(
            UserRoute userRoute,
            FileRoute fileRoute,
            ChatRoute chatRoute,
            ApplicationRoute applicationRoute,
            NoteRoute noteRoute
    ) {
        System.out.println("ss");
        return "OK";
    }
}
