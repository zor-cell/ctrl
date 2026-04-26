<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        <img src="${url.resourcesPath}/img/a-trust-logo.png" alt="A-Trust" class="ida-logo" height="36">
    <#elseif section = "form">
        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

            <div class="pf-c-form__group">
                <label for="username" class="pf-c-form__label">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>${msg("email")}
                    </#if>
                </label>
                <input tabindex="1" id="username" name="username" type="text"
                       autofocus autocomplete="username"
                       value="${(login.username!'')}"
                       class="pf-c-form-control" />
            </div>

            <div class="pf-c-form__group">
                <label for="password" class="pf-c-form__label">${msg("password")}</label>
                <div class="ida-input-wrap">
                    <input tabindex="2" id="password" name="password" type="password"
                           autocomplete="current-password"
                           class="pf-c-form-control" />
                    <button type="button" class="ida-eye-btn"
                            onclick="var p=document.getElementById('password');p.type=p.type==='password'?'text':'password';this.classList.toggle('ida-eye-open');"
                            aria-label="Toggle password visibility">
                        <svg class="ida-eye-off" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                        <svg class="ida-eye-on" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>
                </div>
                <#if realm.resetPasswordAllowed>
                    <a tabindex="5" class="ida-forgot" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                </#if>
            </div>

            <#if realm.rememberMe && !usernameEditDisabled??>
                <div class="pf-c-form__group">
                    <label>
                        <input tabindex="3" type="checkbox" name="rememberMe" <#if login.rememberMe??>checked</#if>>
                        ${msg("rememberMe")}
                    </label>
                </div>
            </#if>

            <input type="hidden" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if> />

            <div class="pf-c-form__group">
                <input tabindex="4" type="submit" id="kc-login" class="ida-btn-identify" value="${msg("doLogIn")}"/>
            </div>

        </form>
    </#if>
</@layout.registrationLayout>
