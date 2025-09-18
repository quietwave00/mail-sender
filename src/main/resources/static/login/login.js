document.getElementById('login-button').addEventListener('click', () => {
    location.href = `${server_host}/oauth2/authorization/google`;
});