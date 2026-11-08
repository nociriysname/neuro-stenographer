# UI-kit «Бумага и небо»

Источник: макет Claude Design, выгружен в [claude-design/](claude-design/). Токены — `tokens.json`, все размеры компонентов — `ds.css` (1 px макета = 1 dp), экраны — `S01…S12*.dc.html`, компоненты — `Components.dc.html` и `Components2.dc.html`.

Модуль: `src/android/sources/common/common-uikit`, пакет `ru.nuclearhack.stenographer.common.uikit`.

## Основа (foundation)

Тема — `StenoTheme { … }`, доступ — `StenoTheme.colors / typography / spacing / shapes / elevation / motion`. Токены — data-классы, отдаются через CompositionLocal; россыпи `const val` нет.

| Что | Класс | Содержимое |
| --- | --- | --- |
| Цвета | `StenoColors` | bg, surface1–3, line, lineStrong, text/text2/text3, primary(+Press, Soft, Ink, Mid), onPrimary, accent(+Soft, Ink), success, warning, error, rec (+Soft/Ink), toast, scrim, switchThumb, `speaker(i)`, onSpeaker, `emotion(EmotionTone)` |
| Текст | `StenoTypography` | display, title, subtitle, body, transcript, bodySmall, label, labelLarge, caption, overline, timer; `TextStyle.tabular()` — табличные цифры |
| Отступы | `StenoSpacing` | s1 = 4 … s16 = 64 dp (номер = число четвёрок), screen = 20 dp |
| Формы | `StenoShapes` | xs 6, sm 10, md 14 (кнопки, поля), lg 18 (карточки), xl 22 (диалог), sheet 26 |
| Тени | `StenoElevation` | card, popup (диалог, тост), sheet — `Modifier.dropShadow(shape, shadow)` |
| Движение | `StenoMotion` | `fast()` 120 мс, `base()` 200, `slow()` 320; `reduced` — системное «убрать анимации» |
| Иконки | `StenoIcons` (enum) | 33 иконки из макета, `StenoIcon(StenoIcons.Mic, …)` |
| Иллюстрации | `StenoIllustrations` (enum) | онбординг ×4, пустая история, ошибки формата, микрофона, модели |

`StenoText` и `StenoIcon` лежат в `components/text`.

## Правила

- Только compose-foundation. Material 3 есть лишь в теме (ripple); в фичах его импорт запрещён detekt'ом.
- Внутри common-uikit сырые dp разрешены: размеры компонента живут в самом компоненте (инлайн или маленький приватный enum размеров). Общие токены не раздуваем ради одного компонента.
- В фичах сырые dp, цвета и доли запрещены (`NoLocalDesignValue`): только `StenoTheme.*` и компоненты.
- Высоты — минимальные (`heightIn(min = …)`), текст в sp: при крупном системном шрифте контролы растут. Зона касания ≥ 48 dp.
- Доступность: роли (`Role.Button`, `Role.Switch`, `Role.RadioButton`), `contentDescription` у иконочных кнопок, `stateDescription` у переключателей.
- Нажатие — `LocalIndication` (ripple темы) плюс цвет pressed из макета через `collectIsPressedAsState`.
- Циклические анимации при `StenoTheme.motion.reduced` становятся статикой.
- У каждого компонента превью в светлой и тёмной темах.

## Каталог компонентов

| Пакет | Компоненты | Экран-источник |
| --- | --- | --- |
| `button` | StenoButton (Primary, Secondary, Text, Danger, DangerText × Large 56, Medium 48, Small 40), StenoIconButton (Outlined, Ghost, Primary, Selected × 48 / 40) | Components |
| `control` | StenoSegmented, StenoSwitch, StenoToggleChip, StenoOptionCard | Components, S04, S11 |
| `field` | StenoSearchField (счётчик «2 из 5», очистка), StenoTextField | Components, S06, S07 |
| `badge` | StenoSpeakerBadge (24 / 32 / 40), StenoEmotionTag, StenoBadge (Neutral, Success, Warning, Error, Rec, Primary), StenoStatusDot | Components |
| `surface` | StenoCard (Raised, Flat), StenoListRow, StenoIconTile, StenoDivider, StenoActionCard | Components, S02, S10 |
| `bar` | StenoTopBar (главная с «глазком», вложенная с «назад»), StenoBottomBar с кнопкой записи в «выемке» | Components2 |
| `overlay` | StenoBottomSheet, StenoDialog, StenoToast + StenoToastHostState | Components2, S07, S09, S12 |
| `scaffold` | StenoScaffold (верхняя и нижняя панели, тосты, отступы системных панелей) | все |
| `state` | StenoStatePanel (иллюстрация, заголовок, текст, действия) | S10, S12 |
| `progress` | StenoLinearProgress, StenoProgressRing, StenoStepper, StenoSkeleton | Components2, S05, S12 |
| `audio` | StenoWaveform, StenoLevelMeter, StenoSpeakerStrip, StenoPlayButton, StenoMiniPlayer, StenoRecordButton | Components2, S03, S06 |
| `timeline` | StenoTimelineRail — вертикальная лента «Диалога» | S06 |
