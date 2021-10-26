# Fusor Control

## Building the JavaDoc

To build the JavaDoc, you need to

> This assumes you are running Linux (I tested this with Ubuntu, but Debian should work too). If you are using a different OS, installing the requirements will be different.
>
> These steps are to public the docs to the `docs` branch on GitHub. To simply test building them, with your own branch as the output, in the `git switch` comand, substitute `docs` for your own branch (e.g., `git switch my_other_branch`). Also, you will need to replace the username (`gh_username` in the script) with your username. You will also need [to generate a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) if you don't already have one. Once you have one, in the script, set the variable `gh_token` to your token. E.g., `gh_token='ghp_XXXXXX'`.

1. Install the requirements with `sudo apt-get update -y && sudo apt-get install git maven openjdk-11-jdk-headless openjdk-11-jre-headless minify -y`.
2. Clone the repo (`git clone https://github.com/EastsidePreparatorySchool/FusorControl`)
3. `cd` into the repo (`cd FusorControl`)
4. Switch to the documentation branch (`git switch docs`)
5. Edit the `buildDocs` file to have `gh_token` be set to the access token for the GitHub user named `fusor-docs-machine-user`. _This token is not public. If you need it, contact me [at my email address](https://mailhide.io/e/WyuHzvS0). [@katelewellen](https://github.com/katelewellen) also has the token for the `fusor-docs-machine-user`._
6. Make the `buildDocs` script executable (`chmod +x ./buildDocs`).
7. Run it (`./buildDocs`). By defualt, this will only build the docs if changes have been detected since the most recent pull. If you want to force a new build, such as to test it, or if the script has been offline for a while, set `force_build` to be `yes`. E.g., `force_build='yes'`
