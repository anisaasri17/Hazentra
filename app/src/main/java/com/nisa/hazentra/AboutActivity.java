package com.nisa.hazentra;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private static final String GITHUB_URL =
            "https://github.com/anisaasri17/Hazentra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        configureToolbar();
        configureGithubSection();
    }

    private void configureToolbar() {

        MaterialToolbar toolbar =
                findViewById(R.id.aboutToolbar);

        toolbar.setTitle("About Hazentra");

        toolbar.setTitleTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.hazentra_on_navy
                )
        );

        toolbar.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void configureGithubSection() {

        TextView githubUrlText =
                findViewById(R.id.githubUrlText);

        MaterialButton openGithubButton =
                findViewById(R.id.openGithubButton);

        githubUrlText.setText(GITHUB_URL);

        githubUrlText.setOnClickListener(
                view -> openGithubRepository()
        );

        openGithubButton.setOnClickListener(
                view -> openGithubRepository()
        );
    }

    private void openGithubRepository() {

        Uri repositoryUri =
                Uri.parse(GITHUB_URL);

        Intent browserIntent =
                new Intent(
                        Intent.ACTION_VIEW,
                        repositoryUri
                );

        try {

            startActivity(browserIntent);

        } catch (ActivityNotFoundException exception) {

            Toast.makeText(
                    this,
                    "No browser application is available.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}