@file:Suppress("ktlint:standard:max-line-length")

package model.chronus

// Данные ContributorAbout в реальном приложении находятся на сервере, поэтому
// устаревшую информацию можно будет обновить для всех пользователей в любой
// момент по вашему запросу

// В реальном приложении ru карточки отображаются, если на телефоне установлен
// русский язык, во всех остальных случаях будут отображены en карточки

enum class Contributor(
	val nickName: String,
	val contributions: Int,
	val contributorAbout: ContributorAbout,
) {
	MXKMN(
		nickName = "mxkmn",
		contributions = 2,
		contributorAbout = ContributorAbout(
			photoUrl = "https://sun9-73.userapi.com/impg/Ejscq12YiM3h6u7GQS_wZRqDjwAgLao1rxbCSA/H0tN5Vs8pYI.jpg?size=2560x1920&quality=96&sign=ecdf89f8515e64dfbc23f6d69443f90e&type=album",
			en = ContributorAboutNameAndCards(
				name = "Maksim Yarkov",
				cards = listOf(
					ContributorAboutCard(
						title = "Found earphones but no music?",
						text = "I like this, it's very grown up:",
						buttons = listOf(
							ContributorAboutButton("Eskimo Callboy", "https://youtu.be/wobbf3lb2nk"),
							ContributorAboutButton("Astroid Boys", "https://youtu.be/-eb8Jp8BjuY"),
							ContributorAboutButton("Infant Annihilator", "https://youtu.be/8dnJpuWuGn8"),
						),
					),
				),
			),
			ru = ContributorAboutNameAndCards(
				name = "Максим Ярков",
				cards = listOf(
					ContributorAboutCard(
						title = "Ты любишь музыку?",
						text = "Лично я люблю музыку, в особенности песни и альбомы",
						buttons = listOf(
							ContributorAboutButton("ПАНЦУШОТ", "https://youtu.be/qzshKdDCw9A"),
							ContributorAboutButton("БАУ", "https://youtu.be/6cIvtibJzAk?t=1556"),
							ContributorAboutButton("svalka", "https://youtu.be/VrJE62WyBm4"),
						),
					),
				),
			),
		),
	),
	YOU(
		nickName = "mimimishkin",
		contributions = 1,
		contributorAbout = ContributorAbout(
			photoUrl = "https://avatars.githubusercontent.com/u/68619081", // фотку можно засунуть из ВК например
			en = ContributorAboutNameAndCards(
				name = "Mimimishka", // добавлять реальное имя необязательно
				cards = listOf(), // оставьте пустым, если с английским плохо - переведу карточки с русского самостоятельно
			),
			ru = ContributorAboutNameAndCards(
				name = "Мимимишка", // добавлять реальное имя необязательно
				cards = listOf(
					ContributorAboutCard(
						title = "Обо мне",
						text = "Пью молоко и читаю статьи на Хабре",
						buttons = listOf(), // карточка может и не содержать ссылок
					),
					ContributorAboutCard(
						title = "Bruhhhhhhhhhhhhh",
						text = "Мяу мяу, мяу мяу; мяу мяу, мяу мяу мяу; мяу мяу мяу мяу мяу...",
						buttons = listOf(), // карточка может и не содержать ссылок
					),
				),
			),
		),
	),
}
