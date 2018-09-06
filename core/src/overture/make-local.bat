@ECHO OFF
SETLOCAL

IF NOT DEFINED ANDROID_NDK_HOME (
	SET ANDROID_NDK_HOME=%ANDROID_HOME%\ndk-bundle
)

SET DIR=%~dp0
SET MIN_API=%1%
SET TARGET=%DIR%\bin
SET DEPS=%DIR%\.deps

SET ANDROID_ARM_TOOLCHAIN=%DEPS%\android-toolchain-%MIN_API%-arm
SET ANDROID_ARM64_TOOLCHAIN=%DEPS%\android-toolchain-%MIN_API%-arm64
SET ANDROID_X86_TOOLCHAIN=%DEPS%\android-toolchain-%MIN_API%-x86

SET ANDROID_ARM_CC=%ANDROID_ARM_TOOLCHAIN%\bin\arm-linux-androideabi-clang
SET ANDROID_ARM_STRIP=%ANDROID_ARM_TOOLCHAIN%\bin\arm-linux-androideabi-strip

SET ANDROID_ARM64_CC=%ANDROID_ARM64_TOOLCHAIN%\bin\aarch64-linux-android-clang
SET ANDROID_ARM64_STRIP=%ANDROID_ARM64_TOOLCHAIN%\bin\aarch64-linux-android-strip

SET ANDROID_X86_CC=%ANDROID_X86_TOOLCHAIN%\bin\i686-linux-android-clang
SET ANDROID_X86_STRIP=%ANDROID_X86_TOOLCHAIN%\bin\i686-linux-android-strip

MKDIR %DEPS%>nul 2>nul 
MKDIR %TARGET%\armeabi-v7a>nul 2>nul 
MKDIR %TARGET%\x86>nul 2>nul 
MKDIR %TARGET%\arm64-v8a>nul 2>nul 

SET CC=%ANDROID_ARM_TOOLCHAIN%\bin\arm-linux-androideabi-gcc.exe
ECHO %ANDROID_X86_CC%
IF NOT EXIST %ANDROID_ARM_CC% (
	ECHO "Make standalone toolchain for ARM arch"
    python.exe %ANDROID_NDK_HOME%\build\tools\make_standalone_toolchain.py --arch arm ^
        --api %MIN_API% --install-dir %ANDROID_ARM_TOOLCHAIN%
)

IF NOT EXIST %ANDROID_ARM64_CC% (
    ECHO "Make standalone toolchain for ARM64 arch"
    python.exe %ANDROID_NDK_HOME%\build\tools\make_standalone_toolchain.py --arch arm64 ^
        --api %MIN_API% --install-dir %ANDROID_ARM64_TOOLCHAIN%
)

IF NOT EXIST %ANDROID_X86_CC% (
    ECHO "Make standalone toolchain for X86 arch"
    python.exe %ANDROID_NDK_HOME%\build\tools\make_standalone_toolchain.py --arch x86 ^
        --api %MIN_API% --install-dir %ANDROID_X86_TOOLCHAIN%
)
REM Check environment availability
IF NOT EXIST %CC% (
    ECHO "gcc not found"
    EXIT 1
)

WHERE python.exe
IF "%ERRORLEVEL%" == 1 (
    ECHO "python not found"
    EXIT 1
)

IF NOT EXIST %DIR%\go\bin\go.exe (
    ECHO "Build the custom go"

    PUSHD %DIR%\go\src
    CALL make.bat
    POPD
)

SET GOROOT=%DIR%\go
SET GOPATH=%DIR%
SET PATH=%GOROOT%\bin;%GOPATH%\bin;%PATH%

SET BUILD=1
IF EXIST "%TARGET%\armeabi-v7a\libss-local.so" (
	IF EXIST "%TARGET%\arm64-v8a\libss-local.so" (
		IF EXIST "%TARGET%\x86\libss-local.so" (
			SET BUILD=0
		)
	)
)
set http_proxy=http://127.0.0.1:1080
IF %BUILD% == 1 (
	ECHO "Get dependences for libss-local"
	go.exe get -u github.com\tools\godep

	PUSHD %GOPATH%\src\github.com\shadowsocks\shadowsocks-go\cmd\shadowsocks-local
	godep.exe restore

	ECHO "Cross compile libss-local for arm"
	IF NOT EXIST "%TARGET%\armeabi-v7a\libss-local.so" (
		SETLOCAL
	    SET CGO_ENABLED=1
	    SET CC=%ANDROID_ARM_CC%
	    SET GOOS=android
	    SET GOARCH=arm
	    SET GOARM=7
	    go.exe build -ldflags="-s -w"
	    %ANDROID_ARM_STRIP% shadowsocks-local
	    MOVE shadowsocks-local %TARGET%\armeabi-v7a\libss-local.so>nul 2>nul 		
	    ENDLOCAL
	)

	ECHO "Cross compile libss-local for arm64"
	IF NOT EXIST "%TARGET%\arm64-v8a\libss-local.so" (
		SETLOCAL
	    SET CGO_ENABLED=1
	    SET CC=%ANDROID_ARM64_CC%
	    SET GOOS=android
	    SET GOARCH=arm64
	    go.exe build -ldflags="-s -w"
	    %ANDROID_ARM64_STRIP% shadowsocks-local
	    MOVE shadowsocks-local %TARGET%\arm64-v8a\libss-local.so>nul 2>nul 
	    ENDLOCAL
	)

	ECHO "Cross compile libss-local for x86"
	IF NOT EXIST "%TARGET%\x86\libss-local.so" (
		SETLOCAL
	    SET CGO_ENABLED=1
	    SET CC=%ANDROID_X86_CC%
	    SET GOOS=android
	    SET GOARCH=386
	    go.exe build -ldflags="-s -w"
	    %ANDROID_X86_STRIP% shadowsocks-local
	    MOVE shadowsocks-local %TARGET%\x86\libss-local.so>nul 2>nul 
	    ENDLOCAL
	)

	POPD
)

ECHO "Successfully build libss-local target:%TARGET%"
ENDLOCAL
