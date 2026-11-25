package com.selah.bible.data

/**
 * Built-in public-domain 1769 King James Version (KJV) starter chapters.
 * Ensures the app, coherent Verse of the Day engine, search, and widgets work
 * immediately out of the box before importing full KJV / NLT EPUB, PDF, or SQLite files.
 */
object StarterKjvData {
    const val TRANSLATION_ID = "kjv"
    const val TRANSLATION_CODE = "KJV"
    const val TRANSLATION_NAME = "King James Version (Starter)"

    data class RawVerse(val bookId: Int, val chapter: Int, val verse: Int, val text: String)

    val verses: List<RawVerse> by lazy {
        buildList {
            fun addChapter(bookId: Int, chapter: Int, vararg lines: String) {
                lines.forEachIndexed { idx, line ->
                    add(RawVerse(bookId, chapter, idx + 1, line))
                }
            }

            // Genesis 1 (Complete)
            addChapter(
                1, 1,
                "In the beginning God created the heaven and the earth.",
                "And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.",
                "And God said, Let there be light: and there was light.",
                "And God saw the light, that it was good: and God divided the light from the darkness.",
                "And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day.",
                "And God said, Let there be a firmament in the midst of the waters, and let it divide the waters from the waters.",
                "And God made the firmament, and divided the waters which were under the firmament from the waters which were above the firmament: and it was so.",
                "And God called the firmament Heaven. And the evening and the morning were the second day.",
                "And God said, Let the waters under the heaven be gathered together unto one place, and let the dry land appear: and it was so.",
                "And God called the dry land Earth; and the gathering together of the waters called he Seas: and God saw that it was good.",
                "And God said, Let the earth bring forth grass, the herb yielding seed, and the fruit tree yielding fruit after his kind, whose seed is in itself, upon the earth: and it was so.",
                "And the earth brought forth grass, and herb yielding seed after his kind, and the tree yielding fruit, whose seed was in itself, after his kind: and God saw that it was good.",
                "And the evening and the morning were the third day.",
                "And God said, Let there be lights in the firmament of the heaven to divide the day from the night; and let them be for signs, and for seasons, and for days, and years:",
                "And let them be for lights in the firmament of the heaven to give light upon the earth: and it was so.",
                "And God made two great lights; the greater light to rule the day, and the lesser light to rule the night: he made the stars also.",
                "And God set them in the firmament of the heaven to give light upon the earth,",
                "And to rule over the day and over the night, and to divide the light from the darkness: and God saw that it was good.",
                "And the evening and the morning were the fourth day.",
                "And God said, Let the waters bring forth abundantly the moving creature that hath life, and fowl that may fly above the earth in the open firmament of heaven.",
                "And God created great whales, and every living creature that moveth, which the waters brought forth abundantly, after their kind, and every winged fowl after his kind: and God saw that it was good.",
                "And God blessed them, saying, Be fruitful, and multiply, and fill the waters in the seas, and let fowl multiply in the earth.",
                "And the evening and the morning were the fifth day.",
                "And God said, Let the earth bring forth the living creature after his kind, cattle, and creeping thing, and beast of the earth after his kind: and it was so.",
                "And God made the beast of the earth after his kind, and cattle after their kind, and every thing that creepeth upon the earth after his kind: and God saw that it was good.",
                "And God said, Let us make man in our image, after our likeness: and let them have dominion over the fish of the sea, and over the fowl of the air, and over the cattle, and over all the earth, and over every creeping thing that creepeth upon the earth.",
                "So God created man in his own image, in the image of God created he him; male and female created he them.",
                "And God blessed them, and God said unto them, Be fruitful, and multiply, and replenish the earth, and subdue it: and have dominion over the fish of the sea, and over the fowl of the air, and over every living thing that moveth upon the earth.",
                "And God said, Behold, I have given you every herb bearing seed, which is upon the face of all the earth, and every tree, in the which is the fruit of a tree yielding seed; to you it shall be for meat.",
                "And to every beast of the earth, and to every fowl of the air, and to every thing that creepeth upon the earth, wherein there is life, I have given every green herb for meat: and it was so.",
                "And God saw every thing that he had made, and, behold, it was very good. And the evening and the morning were the sixth day."
            )

            // Joshua 1:1-9
            addChapter(
                6, 1,
                "Now after the death of Moses the servant of the LORD it came to pass, that the LORD spake unto Joshua the son of Nun, Moses' minister, saying,",
                "Moses my servant is dead; now therefore arise, go over this Jordan, thou, and all this people, unto the land which I do give to them, even to the children of Israel.",
                "Every place that the sole of your foot shall tread upon, that have I given unto you, as I said unto Moses.",
                "From the wilderness and this Lebanon even unto the great river, the river Euphrates, all the land of the Hittites, and unto the great sea toward the going down of the sun, shall be your coast.",
                "There shall not any man be able to stand before thee all the days of thy life: as I was with Moses, so I will be with thee: I will not fail thee, nor forsake thee.",
                "Be strong and of a good courage: for unto this people shalt thou divide for an inheritance the land, which I sware unto their fathers to give them.",
                "Only be thou strong and very courageous, that thou mayest observe to do according to all the law, which Moses my servant commanded thee: turn not from it to the right hand or to the left, that thou mayest prosper whithersoever thou goest.",
                "This book of the law shall not depart out of thy mouth; but thou shalt meditate therein day and night, that thou mayest observe to do according to all that is written therein: for then thou shalt make thy way prosperous, and then thou shalt have good success.",
                "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest."
            )

            // Psalm 1 (Complete)
            addChapter(
                19, 1,
                "Blessed is the man that walketh not in the counsel of the ungodly, nor standeth in the way of sinners, nor sitteth in the seat of the scornful.",
                "But his delight is in the law of the LORD; and in his law doth he meditate day and night.",
                "And he shall be like a tree planted by the rivers of water, that bringeth forth his fruit in his season; his leaf also shall not wither; and whatsoever he doeth shall prosper.",
                "The ungodly are not so: but are like the chaff which the wind driveth away.",
                "Therefore the ungodly shall not stand in the judgment, nor sinners in the congregation of the righteous.",
                "For the LORD knoweth the way of the righteous: but the way of the ungodly shall perish."
            )

            // Psalm 23 (Complete)
            addChapter(
                19, 23,
                "The LORD is my shepherd; I shall not want.",
                "He maketh me to lie down in green pastures: he leadeth me beside the still waters.",
                "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake.",
                "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me.",
                "Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil; my cup runneth over.",
                "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the LORD for ever."
            )

            // Psalm 27 (Complete)
            addChapter(
                19, 27,
                "The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?",
                "When the wicked, even mine enemies and my foes, came upon me to eat up my flesh, they stumbled and fell.",
                "Though an host should encamp against me, my heart shall not fear: though war should rise against me, in this will I be confident.",
                "One thing have I desired of the LORD, that will I seek after; that I may dwell in the house of the LORD all the days of my life, to behold the beauty of the LORD, and to enquire in his temple.",
                "For in the time of trouble he shall hide me in his pavilion: in the secret of his tabernacle shall he hide me; he shall set me up upon a rock.",
                "And now shall mine head be lifted up above mine enemies round about me: therefore will I offer in his tabernacle sacrifices of joy; I will sing, yea, I will sing praises unto the LORD.",
                "Hear, O LORD, when I cry with my voice: have mercy also upon me, and answer me.",
                "When thou saidst, Seek ye my face; my heart said unto thee, Thy face, LORD, will I seek.",
                "Hide not thy face far from me; put not thy servant away in anger: thou hast been my help; leave me not, neither forsake me, O God of my salvation.",
                "When my father and my mother forsake me, then the LORD will take me up.",
                "Teach me thy way, O LORD, and lead me in a plain path, because of mine enemies.",
                "Deliver me not over unto the will of mine enemies: for false witnesses are risen up against me, and such as breathe out cruelty.",
                "I had fainted, unless I had believed to see the goodness of the LORD in the land of the living.",
                "Wait on the LORD: be of good courage, and he shall strengthen thine heart: wait, I say, on the LORD."
            )

            // Psalm 46 (Complete)
            addChapter(
                19, 46,
                "God is our refuge and strength, a very present help in trouble.",
                "Therefore will not we fear, though the earth be removed, and though the mountains be carried into the midst of the sea;",
                "Though the waters thereof roar and be troubled, though the mountains shake with the swelling thereof. Selah.",
                "There is a river, the streams whereof shall make glad the city of God, the holy place of the tabernacles of the most High.",
                "God is in the midst of her; she shall not be moved: God shall help her, and that right early.",
                "The heathen raged, the kingdoms were moved: he uttered his voice, the earth melted.",
                "The LORD of hosts is with us; the God of Jacob is our refuge. Selah.",
                "Come, behold the works of the LORD, what desolations he hath made in the earth.",
                "He maketh wars to cease unto the end of the earth; he breaketh the bow, and cutteth the spear in sunder; he burneth the chariot in the fire.",
                "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.",
                "The LORD of hosts is with us; the God of Jacob is our refuge. Selah."
            )

            // Psalm 91 (Complete)
            addChapter(
                19, 91,
                "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.",
                "I will say of the LORD, He is my refuge and my fortress: my God; in him will I trust.",
                "Surely he shall deliver thee from the snare of the fowler, and from the noisome pestilence.",
                "He shall cover thee with his feathers, and under his wings shalt thou trust: his truth shall be thy shield and buckler.",
                "Thou shalt not be afraid for the terror by night; nor for the arrow that flieth by day;",
                "Nor for the pestilence that walketh in darkness; nor for the destruction that wasteth at noonday.",
                "A thousand shall fall at thy side, and ten thousand at thy right hand; but it shall not come nigh thee.",
                "Only with thine eyes shalt thou behold and see the reward of the wicked.",
                "Because thou hast made the LORD, which is my refuge, even the most High, thy habitation;",
                "There shall no evil befall thee, neither shall any plague come nigh thy dwelling.",
                "For he shall give his angels charge over thee, to keep thee in all thy ways.",
                "They shall bear thee up in their hands, lest thou dash thy foot against a stone.",
                "Thou shalt tread upon the lion and adder: the young lion and the dragon shalt thou trample under feet.",
                "Because he hath set his love upon me, therefore will I deliver him: I will set him on high, because he hath known my name.",
                "He shall call upon me, and I will answer him: I will be with him in trouble; I will deliver him, and honour him.",
                "With long life will I satisfy him, and shew him my salvation."
            )

            // Psalm 121 (Complete)
            addChapter(
                19, 121,
                "I will lift up mine eyes unto the hills, from whence cometh my help.",
                "My help cometh from the LORD, which made heaven and earth.",
                "He will not suffer thy foot to be moved: he that keepeth thee will not slumber.",
                "Behold, he that keepeth Israel shall neither slumber nor sleep.",
                "The LORD is thy keeper: the LORD is thy shade upon thy right hand.",
                "The sun shall not smite thee by day, nor the moon by night.",
                "The LORD shall preserve thee from all evil: he shall preserve thy soul.",
                "The LORD shall preserve thy going out and thy coming in from this time forth, and even for evermore."
            )

            // Proverbs 3:1-18
            addChapter(
                20, 3,
                "My son, forget not my law; but let thine heart keep my commandments:",
                "For length of days, and long life, and peace, shall they add to thee.",
                "Let not mercy and truth forsake thee: bind them about thy neck; write them upon the table of thine heart:",
                "So shalt thou find favour and good understanding in the sight of God and man.",
                "Trust in the LORD with all thine heart; and lean not unto thine own understanding.",
                "In all thy ways acknowledge him, and he shall direct thy paths.",
                "Be not wise in thine own eyes: fear the LORD, and depart from evil.",
                "It shall be health to thy navel, and marrow to thy bones.",
                "Honour the LORD with thy substance, and with the firstfruits of all thine increase:",
                "So shall thy barns be filled with plenty, and thy presses shall burst out with new wine.",
                "My son, despise not the chastening of the LORD; neither be weary of his correction:",
                "For whom the LORD loveth he correcteth; even as a father the son in whom he delighteth.",
                "Happy is the man that findeth wisdom, and the man that getteth understanding.",
                "For the merchandise of it is better than the merchandise of silver, and the gain thereof than fine gold.",
                "She is more precious than rubies: and all the things thou canst desire are not to be compared unto her.",
                "Length of days is in her right hand; and in her left hand riches and honour.",
                "Her ways are ways of pleasantness, and all her paths are peace.",
                "She is a tree of life to them that lay hold upon her: and happy is every one that retaineth her."
            )

            // Isaiah 40:25-31
            add(RawVerse(23, 40, 28, "Hast thou not known? hast thou not heard, that the everlasting God, the LORD, the Creator of the ends of the earth, fainteth not, neither is weary? there is no searching of his understanding."))
            add(RawVerse(23, 40, 29, "He giveth power to the faint; and to them that have no might he increaseth strength."))
            add(RawVerse(23, 40, 30, "Even the youths shall faint and be weary, and the young men shall utterly fall:"))
            add(RawVerse(23, 40, 31, "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."))

            // Isaiah 41:10-13
            add(RawVerse(23, 41, 10, "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee; yea, I will help thee; yea, I will uphold thee with the right hand of my righteousness."))
            add(RawVerse(23, 41, 13, "For I the LORD thy God will hold thy right hand, saying unto thee, Fear not; I will help thee."))

            // Jeremiah 29:11-13
            add(RawVerse(24, 29, 11, "For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end."))
            add(RawVerse(24, 29, 12, "Then shall ye call upon me, and ye shall go and pray unto me, and I will hearken unto you."))
            add(RawVerse(24, 29, 13, "And ye shall seek me, and find me, when ye shall search for me with all your heart."))

            // Matthew 5:1-16 (Beatitudes & Salt/Light)
            addChapter(
                40, 5,
                "And seeing the multitudes, he went up into a mountain: and when he was set, his disciples came unto him:",
                "And he opened his mouth, and taught them, saying,",
                "Blessed are the poor in spirit: for theirs is the kingdom of heaven.",
                "Blessed are they that mourn: for they shall be comforted.",
                "Blessed are the meek: for they shall inherit the earth.",
                "Blessed are they which do hunger and thirst after righteousness: for they shall be filled.",
                "Blessed are the merciful: for they shall obtain mercy.",
                "Blessed are the pure in heart: for they shall see God.",
                "Blessed are the peacemakers: for they shall be called the children of God.",
                "Blessed are they which are persecuted for righteousness' sake: for theirs is the kingdom of heaven.",
                "Blessed are ye, when men shall revile you, and persecute you, and shall say all manner of evil against you falsely, for my sake.",
                "Rejoice, and be exceeding glad: for great is your reward in heaven: for so persecuted they the prophets which were before you.",
                "Ye are the salt of the earth: but if the salt have lost his savour, wherewith shall it be salted? it is thenceforth good for nothing, but to be cast out, and to be trodden under foot of men.",
                "Ye are the light of the world. A city that is set on an hill cannot be hid.",
                "Neither do men light a candle, and put it under a bushel, but on a candlestick; and it giveth light unto all that are in the house.",
                "Let your light so shine before men, that they may see your good works, and glorify your Father which is in heaven."
            )

            // Matthew 6:25-34
            add(RawVerse(40, 6, 25, "Therefore I say unto you, Take no thought for your life, what ye shall eat, or what ye shall drink; nor yet for your body, what ye shall put on. Is not the life more than meat, and the body than raiment?"))
            add(RawVerse(40, 6, 26, "Behold the fowls of the air: for they sow not, neither do they reap, nor gather into barns; yet your heavenly Father feedeth them. Are ye not much better than they?"))
            add(RawVerse(40, 6, 27, "Which of you by taking thought can add one cubit unto his stature?"))
            add(RawVerse(40, 6, 33, "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you."))
            add(RawVerse(40, 6, 34, "Take therefore no thought for the morrow: for the morrow shall take thought for the things of itself. Sufficient unto the day is the evil thereof."))

            // John 1:1-14
            addChapter(
                43, 1,
                "In the beginning was the Word, and the Word was with God, and the Word was God.",
                "The same was in the beginning with God.",
                "All things were made by him; and without him was not any thing made that was made.",
                "In him was life; and the life was the light of men.",
                "And the light shineth in darkness; and the darkness comprehended it not.",
                "There was a man sent from God, whose name was John.",
                "The same came for a witness, to bear witness of the Light, that all men through him might believe.",
                "He was not that Light, but was sent to bear witness of that Light.",
                "That was the true Light, which lighteth every man that cometh into the world.",
                "He was in the world, and the world was made by him, and the world knew him not.",
                "He came unto his own, and his own received him not.",
                "But as many as received him, to them gave he power to become the sons of God, even to them that believe on his name:",
                "Which were born, not of blood, nor of the will of the flesh, nor of the will of man, but of God.",
                "And the Word was made flesh, and dwelt among us, (and we beheld his glory, the glory as of the only begotten of the Father,) full of grace and truth."
            )

            // John 3:16-17
            add(RawVerse(43, 3, 16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life."))
            add(RawVerse(43, 3, 17, "For God sent not his Son into the world to condemn the world; but that the world through him might be saved."))

            // John 14:1-6, 27
            addChapter(
                43, 14,
                "Let not your heart be troubled: ye believe in God, believe also in me.",
                "In my Father's house are many mansions: if it were not so, I would have told you. I go to prepare a place for you.",
                "And if I go and prepare a place for you, I will come again, and receive you unto myself; that where I am, there ye may be also.",
                "And whither I go ye know, and the way ye know.",
                "Thomas saith unto him, Lord, we know not whither thou goest; and how can we know the way?",
                "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me."
            )
            add(RawVerse(43, 14, 27, "Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid."))

            // Romans 8:1-2, 28, 31, 38-39
            add(RawVerse(45, 8, 1, "There is therefore now no condemnation to them which are in Christ Jesus, who walk not after the flesh, but after the Spirit."))
            add(RawVerse(45, 8, 28, "And we know that all things work together for good to them that love God, to them who are the called according to his purpose."))
            add(RawVerse(45, 8, 31, "What shall we then say to these things? If God be for us, who can be against us?"))
            add(RawVerse(45, 8, 38, "For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come,"))
            add(RawVerse(45, 8, 39, "Nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord."))

            // Romans 12:1-2, 9-12
            add(RawVerse(45, 12, 1, "I beseech you therefore, brethren, by the mercies of God, that ye present your bodies a living sacrifice, holy, acceptable unto God, which is your reasonable service."))
            add(RawVerse(45, 12, 2, "And be not conformed to this world: but be ye transformed by the renewing of your mind, that ye may prove what is that good, and acceptable, and perfect, will of God."))
            add(RawVerse(45, 12, 9, "Let love be without dissimulation. Abhor that which is evil; cleave to that which is good."))
            add(RawVerse(45, 12, 12, "Rejoicing in hope; patient in tribulation; continuing instant in prayer;"))

            // 1 Corinthians 13 (Complete)
            addChapter(
                46, 13,
                "Though I speak with the tongues of men and of angels, and have not charity, I am become as sounding brass, or a tinkling cymbal.",
                "And though I have the gift of prophecy, and understand all mysteries, and all knowledge; and though I have all faith, so that I could remove mountains, and have not charity, I am nothing.",
                "And though I bestow all my goods to feed the poor, and though I give my body to be burned, and have not charity, it profiteth me nothing.",
                "Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up,",
                "Doth not behave itself unseemly, seeketh not her own, is not easily provoked, thinketh no evil;",
                "Rejoiceth not in iniquity, but rejoiceth in the truth;",
                "Beareth all things, believeth all things, hopeth all things, endureth all things.",
                "Charity never faileth: but whether there be prophecies, they shall fail; whether there be tongues, they shall cease; whether there be knowledge, it shall vanish away.",
                "For we know in part, and we prophesy in part.",
                "But when that which is perfect is come, then that which is in part shall be done away.",
                "When I was a child, I spake as a child, I understood as a child, I thought as a child: but when I became a man, I put away childish things.",
                "For now we see through a glass, darkly; but then face to face: now I know in part; but then shall I know even as also I am known.",
                "And now abideth faith, hope, charity, these three; but the greatest of these is charity."
            )

            // Philippians 4:4-13
            add(RawVerse(50, 4, 4, "Rejoice in the Lord alway: and again I say, Rejoice."))
            add(RawVerse(50, 4, 5, "Let your moderation be known unto all men. The Lord is at hand."))
            add(RawVerse(50, 4, 6, "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God."))
            add(RawVerse(50, 4, 7, "And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus."))
            add(RawVerse(50, 4, 8, "Finally, brethren, whatsoever things are true, whatsoever things are honest, whatsoever things are just, whatsoever things are pure, whatsoever things are lovely, whatsoever things are of good report; if there be any virtue, and if there be any praise, think on these things."))
            add(RawVerse(50, 4, 13, "I can do all things through Christ which strengtheneth me."))
            add(RawVerse(50, 4, 19, "But my God shall supply all your need according to his riches in glory by Christ Jesus."))

            // James 1:2-8, 17
            add(RawVerse(59, 1, 2, "My brethren, count it all joy when ye fall into divers temptations;"))
            add(RawVerse(59, 1, 3, "Knowing this, that the trying of your faith worketh patience."))
            add(RawVerse(59, 1, 4, "But let patience have her perfect work, that ye may be perfect and entire, wanting nothing."))
            add(RawVerse(59, 1, 5, "If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him."))
            add(RawVerse(59, 1, 17, "Every good gift and every perfect gift is from above, and cometh down from the Father of lights, with whom is no variableness, neither shadow of turning."))

            // Revelation 21:1-5
            addChapter(
                66, 21,
                "And I saw a new heaven and a new earth: for the first heaven and the first earth were passed away; and there was no more sea.",
                "And I John saw the holy city, new Jerusalem, coming down from God out of heaven, prepared as a bride adorned for her husband.",
                "And I heard a great voice out of heaven saying, Behold, the tabernacle of God is with men, and he will dwell with them, and they shall be his people, and God himself shall be with them, and be their God.",
                "And God shall wipe away all tears from their eyes; and there shall be no more death, neither sorrow, nor crying, neither shall there be any more pain: for the former things are passed away.",
                "And he that sat upon the throne said, Behold, I make all things new. And he said unto me, Write: for these words are true and faithful."
            )
        }
    }
}
