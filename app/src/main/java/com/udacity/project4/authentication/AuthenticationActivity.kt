package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    // Get a reference to the ViewModel scoped to this Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding : ActivityAuthenticationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        //TODO: (Ok) Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google

        //TODO: (Ok) If the user was authenticated, send him to RemindersActivity

        //TODO: (Ok) a bonus is to customize the sign in flow to look nice using :

        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout


        binding.authButton.setOnClickListener {
            launchSignInFlow()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SIGN_IN_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun launchSignInFlow(){
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val builder : AuthUI.SignInIntentBuilder = AuthUI
            .getInstance()
            .createSignInIntentBuilder()


        val customLayout = AuthMethodPickerLayout.Builder(R.layout.custom_signin)
            .setGoogleButtonId(R.id.buttonGoogle)
            .setEmailButtonId(R.id.buttonEmail)
            .build()
        startActivityForResult(builder.setAuthMethodPickerLayout(customLayout)
            .setAvailableProviders(providers)
            .setTheme(R.style.AppTheme)
            .setIsSmartLockEnabled(false)
            .build(), SIGN_IN_REQUEST_CODE)
    }


    companion object {
        const val TAG = "MainFragment"
        const val SIGN_IN_REQUEST_CODE = 1001
    }
}
