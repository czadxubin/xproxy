:: 设置SDK根目录和版本号
set "WindowsSdkDir=C:\Program Files (x86)\Windows Kits\10\"
set "WindowsSdkVersion=10.0.18362.0"

:: 将SDK的bin目录添加到PATH（优先使用x64架构）
set "PATH=%WindowsSdkDir%bin\%WindowsSdkVersion%\x64;%PATH%"

:: 可选：如果需要指定架构（如x86）
:: set "PATH=%WindowsSdkDir%bin\%WindowsSdkVersion%\x86;%PATH%"