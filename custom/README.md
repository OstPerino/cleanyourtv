# Телевизор в комнате: Haier Android TV FF Pro

Личная сборка на основе [CleanYourTV](https://github.com/Gathin23/cleanyourtv), ветка `custom`.
Модель `Haier Android TV FF Pro` (device `daan`), Android TV 14, прошивка `UKNV.260410.001`, 1,8 ГБ памяти,
панель 4K, интерфейс рисуется в 1920×1080.

Все команды ниже запускаются из корня репозитория `~/Projects/tv-boost/cleanyourtv`.

## Подключение к телевизору

macOS не пускает Claude Code в локальную сеть, поэтому соединение идёт через relay.

1. На телевизоре: Настройки → Для разработчиков → «Отладка по USB» и «Отладка по сети» включены.
2. Узнать IP телевизора: Настройки → Сеть → текущая сеть. Адрес меняется при смене сети (2,4 / 5 ГГц).
3. В обычном Терминале, окно не закрывать:
   ```
   tools/venv/bin/python -I tools/relay.py <IP телевизора>
   ```
4. Во всех командах ниже: `export TV_HOST=127.0.0.1 TV_PORT=15555`.

Проверка: `tools/venv/bin/python -I tools/tv.py --probe` печатает модель.

## Что сделано

### Отключено (не удалено)

Список — [`debloat-ff-pro.txt`](debloat-ff-pro.txt): слежка за просмотром (Anoki ACR), магазинное демо,
сервисы и чистильщик Haier, Haier Evo, игровая панель, Prime Video, YouTube Music, Play Игры, Netflix,
FAST-каналы. Netflix и FAST-каналы прошивка включает обратно при каждой загрузке — их плитки скрыты в лаунчере.

Не тронуто: Play Маркет и Play Services, YouTube, голосовой ассистент, Chromecast, Miracast/HaiCast,
обновление прошивки, HDMI, тюнер, пульт и Bluetooth.

### Удалено по просьбе владельца

Kinopub, Filmix, Premier, Vimu, Nova, WireGuard. Это были обычные приложения, системные не трогались.
Вернуть — поставить заново из Play Маркета или из APK.

### Главный экран

Лаунчер `com.gathin.tvhome`, собран из `launcher/` этой ветки. Отличия от исходного проекта:

| Что | Где |
|---|---|
| Видеофон или лёгкая анимация, переключаются на телевизоре | `Backdrop.java`, `AuroraView.java` |
| Погода в шапке | `Weather.java`, город и координаты в `launcher/local.properties` |
| Плитка «Фильмы» — сразу папка в VLC | `MoviesActivity.java`, адрес в `launcher/local.properties` |
| Анимированный фон в заставке (через 10 минут простоя) | `ClockScene.java`, `dream_clock.xml` |
| Док: Кинопоиск, YouTube, Фильмы, VK Видео, zaprett | `res/values/config.xml`, `dock_apps` |
| Кнопка «+» (библиотека приложений) в конце экрана | `config.xml`, `app_library` |

Погода берётся с `api.met.no`: Open-Meteo с этого провайдера недоступен (блокируется хостинг Hetzner).

Личные значения — адрес папки с фильмами, город и координаты — лежат в `launcher/local.properties`,
который не попадает в git. Шаблон — [`local.properties.example`](local.properties.example): скопировать строки
в `launcher/local.properties` и поменять. Пустое значение выключает погоду или плитку.

Пересобрать и поставить после правок:
```
cd launcher && JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools bash build-and-install.sh
```

### Фон

На телевизоре: зажать OK на «Настройки» → «Настройки главного экрана» → строка **Фон**. Каждое нажатие
переключает: Видео → Лёгкая анимация → Выключен.

Свой ролик (любой формат; обрежется до 60 с, перекодируется в 1080p без звука):
```
custom/backdrop.sh ~/Movies/мой-фон.mp4
custom/backdrop.sh --remove          # убрать видео, останется анимация
```
Ролик лучше брать тёмный и спокойный, с совпадающими первым и последним кадром — он крутится по кругу.
Стандартный фон сгенерирован [`aurora.py`](aurora.py).

Замеры на этом телевизоре: видео держит ~230 МБ памяти, плавность меню у видео и анимации одинаковая
(кадр ~25 мс); без фона кадр ~13 мс.

### Zaprett (YouTube)

`com.cherret.zaprett` 3.2.0 из [официального релиза](https://github.com/CherretGit/zaprett-app/releases),
SHA-256 `125cf9d9d934fbfb1a42e8a7d47a9f77209b81efa9b0d1413cabc32a898ebbed`.
Root на телевизоре нет, режим — **byedpi** (через системный VPN, в шторке значок ключа).

Установлены стратегии: `strategy-yt-new-fix` (**включена**), `strategy-yt-ds-r34`,
`strategy-youtube-fix-mts`, `strategy-ultimatefix`, `strategy-test-speed-2`.

Сменить стратегию: zaprett → вкладка «Стратегии» → выключить текущую, включить другую →
«Главная» → «Остановить сервис» → «Запустить сервис». Новые — «Стратегии» → «+» → «Скачать».

Если YouTube перестал открываться — первым делом перезапустить сервис, потом пробовать другие стратегии.

**Автозапуск.** У zaprett нет своего запуска при загрузке, поэтому он назначен системным «постоянным VPN»:
```
tools/venv/bin/python -I tools/tv.py 'settings put secure always_on_vpn_app com.cherret.zaprett; settings put secure always_on_vpn_lockdown 0'
```
Проверено полным выключением телевизора: VPN поднимается сам. Отменить —
`settings delete secure always_on_vpn_app`. `lockdown 0` значит, что без VPN интернет не блокируется.

**Погода и VPN.** Погода берётся с `api.met.no` и работает и через zaprett, и без него.

### Google Play

27 октября Маркет перестал пускать: «нет подключения к Wi-Fi» при рабочей сети. Причина не в сети и не в
zaprett: в логе `Auth: BadAuthentication` / `AuthFailureError: User needs to (re)enter credentials` — Google
перестал принимать сохранённый на телевизоре вход в аккаунт. Включение обратно `partnersetup` ошибку
не меняло (проверено). Аккаунт Google с телевизора удалён; YouTube залогинен своим входом.

Без аккаунта Маркет не работает и приложения не обновляются сами. Вернуть: Настройки → Аккаунты и вход →
Добавить аккаунт → Google, на экране пароля — «Другой способ входа» → «Нажмите „Да“ на телефоне».
Как смотреть причину самому:
```
tools/venv/bin/python -I tools/tv.py 'logcat -d | grep -E "BadAuthentication|enter credentials" | tail'
```

### Плитка «Фильмы»

Плитка запускает сетевой браузер VLC (`org.videolan.television.ui.browser.VerticalGridActivity`) с адресом
`tv.movies_uri` и `browser_type = 3L`. Проверено на VLC 3.6.5, в том числе после `am force-stop org.videolan.vlc`.

Папка — общий ресурс Windows `Movies`, доступ только на чтение отдельному пользователю `tvviewer`.
Пароль хранит VLC на телевизоре («Запомнить пароль»), в лаунчере его нет.

Настройка на Windows (PowerShell от администратора, путь — свой):
```powershell
$folder = "C:\путь\к\фильмам"
$pass = Read-Host -AsSecureString "Пароль для tvviewer"
New-LocalUser -Name tvviewer -Password $pass -PasswordNeverExpires -AccountNeverExpires -Description "TV, только чтение"
New-SmbShare -Name Movies -Path $folder -ReadAccess tvviewer
icacls $folder /grant "tvviewer:(OI)(CI)RX" /T | Out-Null
Get-NetConnectionProfile | Set-NetConnectionProfile -NetworkCategory Private
Enable-NetFirewallRule -Group "@FirewallAPI.dll,-28502"
```
IP компьютера закрепить в роутере (резервирование DHCP), иначе плитка потеряет папку. Компьютер должен
быть включён и не спать. Сменить пароль — `net user tvviewer *`; VLC при неудачном входе спросит пароль
снова. Если не спросит — Настройки телевизора → Приложения → VLC → «Очистить данные» (сбросит и историю VLC).

## Откат

| Что вернуть | Команда |
|---|---|
| Отключённые пакеты | `tools/restore.sh custom/debloat-ff-pro.txt com.google.android.tvlauncher` |
| Штатный главный экран | `tools/venv/bin/python -I tools/tv.py 'pm enable --user 0 com.google.android.tvlauncher'`, затем на телевизоре выбрать его по кнопке Home, и `pm uninstall com.gathin.tvhome` |
| Тема (прозрачность дока) | `tools/theme.sh --reset` |
| Zaprett | Настройки телевизора → Приложения → zaprett → Удалить |

Без компьютера: Настройки → Приложения → «Показать системные» → нужное приложение → «Включить».
Ничего системного не удалялось, сброс к заводским настройкам вернёт всё как было.

Резервная копия до изменений — `backup/` (пакеты, getprop, все settings, Home). Записи `tools/disable.sh`
о каждой пачке — `~/.cleanyourtv/recovery-*.json`. Обе папки не коммитятся: в них идентификаторы телевизора.

## После работы

Отладку по сети оставлять открытой нельзя: порт 5555 без шифрования. После завершения настройки:
Для разработчиков → выключить «Отладка по USB» и «Отладка по сети», «Отозвать доступ для отладки»,
перезагрузить телевизор. Проверка из обычного Терминала: `tools/venv/bin/python -I tools/adb-closed.py <IP>`.
Чтобы снова что-то поменять — включить отладку обратно и начать с раздела «Подключение».
