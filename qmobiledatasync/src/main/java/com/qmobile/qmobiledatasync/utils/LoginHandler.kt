/*
 * Created by qmarciset on 23/11/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

interface LoginHandler {

    /**
     * Tells whether you want to ensure that the input is a valid mail address
     */
    val ensureValidMail: Boolean

    /**
     * Init your view components here
     */
    fun initLayout()

    /**
     * Custom input validation before login
     * Return true if you don't need extra validation
     */
    fun validate(input: String): Boolean

    /**
     * Triggered before login if the input is not valid
     */
    fun onInputInvalid()

    /**
     * Triggered when login before login request starts
     */
    fun onLoginInProgress(inProgress: Boolean)

    /**
     * Triggered on successful login
     */
    fun onLoginSuccessful()

    /**
     * Triggered on unsuccessful login
     */
    fun onLoginUnsuccessful()

    /**
     * Triggered after a logout
     */
    fun onLogout()
}

annotation class LoginForm
