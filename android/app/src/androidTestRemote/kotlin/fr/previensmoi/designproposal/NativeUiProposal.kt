package fr.previensmoi.designproposal

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import fr.previensmoi.R

/** Visual proposal only. Lives in the instrumentation APK, with no repository or network access. */
internal enum class ProposalPage { Home, Loading, Active, Preparing, Paused }
internal enum class ProposalState(val label: String, val icon: Int) {
    Active("Actif", R.drawable.symbol_check_circle),
    Preparing("En préparation", R.drawable.symbol_schedule),
    Paused("En pause", R.drawable.symbol_pause),
}

private val Light = lightColorScheme(
    primary=Color(0xFF303D34), onPrimary=Color.White,
    primaryContainer=Color(0xFFE0E8DD), onPrimaryContainer=Color(0xFF26352A),
    secondary=Color(0xFF586456), onSecondary=Color.White,
    secondaryContainer=Color(0xFFE7EBE2), onSecondaryContainer=Color(0xFF303D34),
    tertiary=Color(0xFF765B14), onTertiary=Color.White,
    tertiaryContainer=Color(0xFFF4D38B), onTertiaryContainer=Color(0xFF302406),
    background=Color(0xFFF6F6F0), onBackground=Color(0xFF222820),
    surface=Color(0xFFF6F6F0), onSurface=Color(0xFF222820),
    surfaceContainerLowest=Color.White, surfaceContainerLow=Color(0xFFF0F1E9),
    surfaceContainer=Color(0xFFECEEE4), surfaceContainerHigh=Color(0xFFE5E8DD),
    surfaceContainerHighest=Color(0xFFDFE3D7),
    onSurfaceVariant=Color(0xFF5C645A), outline=Color(0xFF767E71), outlineVariant=Color(0xFFD8DED1),
)
private val Dark = darkColorScheme(
    primary=Color(0xFFDCE6D8), onPrimary=Color(0xFF243022),
    primaryContainer=Color(0xFF354132), onPrimaryContainer=Color(0xFFDAE5D6),
    secondary=Color(0xFFC0CBBA), onSecondary=Color(0xFF2B3627),
    secondaryContainer=Color(0xFF364332), onSecondaryContainer=Color(0xFFDFE9D8),
    tertiary=Color(0xFFE5C56F), onTertiary=Color(0xFF3C2E04),
    tertiaryContainer=Color(0xFF544319), onTertiaryContainer=Color(0xFFFFE4A3),
    background=Color(0xFF171C17), onBackground=Color(0xFFE0E6DB),
    surface=Color(0xFF171C17), onSurface=Color(0xFFE0E6DB),
    surfaceContainerLowest=Color(0xFF111611), surfaceContainerLow=Color(0xFF202620),
    surfaceContainer=Color(0xFF252C24), surfaceContainerHigh=Color(0xFF2C342B),
    surfaceContainerHighest=Color(0xFF374035),
    onSurfaceVariant=Color(0xFFBFC9B9), outline=Color(0xFF899582), outlineVariant=Color(0xFF414B3D),
)

@Composable
internal fun NativeUiProposal(page: ProposalPage, dark: Boolean = false, onPage: (ProposalPage)->Unit = {}) {
    val view=LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let {
            WindowCompat.getInsetsController(it,view).apply {
                isAppearanceLightStatusBars=!dark
                isAppearanceLightNavigationBars=!dark
            }
        }
    }
    MaterialTheme(colorScheme=if(dark) Dark else Light) {
        ProposalScaffold(page, dark, onPage)
    }
}

@Composable private fun Symbol(id: Int, modifier: Modifier = Modifier, description: String? = null) {
    Icon(painterResource(id),description,modifier.size(24.dp))
}
@Composable private fun Status(state: ProposalState, dark: Boolean, compact: Boolean = false) {
    val (foreground,background)=when(state) {
        ProposalState.Active -> if(dark) Color(0xFF98DBB7) to Color(0xFF223F2E) else Color(0xFF186544) to Color(0xFFE1F2E7)
        ProposalState.Preparing -> if(dark) Color(0xFFECCC83) to Color(0xFF43371C) else Color(0xFF805600) to Color(0xFFFFF0C9)
        ProposalState.Paused -> if(dark) Color(0xFFC3CEC0) to Color(0xFF343E31) else Color(0xFF555F52) to Color(0xFFE8EDE3)
    }
    Surface(color=background, contentColor=foreground, shape=RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal=10.dp,vertical=if(compact) 5.dp else 7.dp),
            horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) {
            Symbol(state.icon, modifier=Modifier.size(if(compact) 16.dp else 18.dp))
            Text(state.label,style=if(compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ProposalScaffold(page: ProposalPage, dark: Boolean, onPage: (ProposalPage)->Unit) {
    val home=page==ProposalPage.Home || page==ProposalPage.Loading
    Scaffold(
        modifier=Modifier.testTag("proposal_root"), containerColor=MaterialTheme.colorScheme.background,
        topBar={
            Column {
                if(home) MediumTopAppBar(
                    title={Text("Mes suivis",fontWeight=FontWeight.SemiBold)},
                    actions={
                        IconButton(onClick={onPage(ProposalPage.Loading)}) {Symbol(R.drawable.symbol_refresh,description="Actualiser")}
                        FilledTonalIconButton(onClick={},modifier=Modifier.padding(end=12.dp).semantics {contentDescription="Mon compte"}) {
                            Text("FB",style=MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),
                ) else TopAppBar(
                    title={Text("Détail du suivi",style=MaterialTheme.typography.titleLarge)},
                    navigationIcon={IconButton(onClick={onPage(ProposalPage.Home)}) {Symbol(R.drawable.symbol_arrow_back,description="Retour")}},
                    actions={
                        IconButton(onClick={}) {Symbol(R.drawable.symbol_refresh,description="Actualiser")}
                        IconButton(onClick={}) {Symbol(R.drawable.symbol_more_vert,description="Actions du suivi")}
                    },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),
                )
                if(page==ProposalPage.Loading) LinearProgressIndicator(
                    modifier=Modifier.fillMaxWidth().height(3.dp),
                    color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        },
        floatingActionButton={if(home) ExtendedFloatingActionButton(
            onClick={},icon={Symbol(R.drawable.symbol_add)},text={Text("Nouveau suivi")},
            containerColor=MaterialTheme.colorScheme.tertiaryContainer,
            contentColor=MaterialTheme.colorScheme.onTertiaryContainer,
        )},
    ) { insets ->
        if(home) HomeContent(page==ProposalPage.Loading,dark,Modifier.padding(insets),onPage)
        else DetailContent(page,dark,Modifier.padding(insets),onPage)
    }
}

@Composable private fun HomeContent(loading:Boolean,dark:Boolean,modifier:Modifier,onPage:(ProposalPage)->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(top=18.dp,bottom=100.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        if(loading) Placeholder(Modifier.width(65.dp).height(14.dp))
        else Text("3 suivis",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest,shape=RoundedCornerShape(20.dp)) {
            Column {
                if(loading) repeat(3) {i->
                    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Placeholder(Modifier.fillMaxWidth(if(i==1) .85f else .68f).height(18.dp))
                        Placeholder(Modifier.width(124.dp).height(25.dp))
                        Placeholder(Modifier.width(90.dp).height(12.dp))
                    }
                    if(i<2) HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outlineVariant)
                } else {
                    WatchRow("Promotions PlayStation 5",ProposalState.Active,"Vérifié à 09:41",dark) {onPage(ProposalPage.Active)}
                    HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outlineVariant)
                    WatchRow("Offres d’emploi Android à Montpellier",ProposalState.Preparing,null,dark) {onPage(ProposalPage.Preparing)}
                    HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outlineVariant)
                    WatchRow("Sortie du prochain iPhone",ProposalState.Paused,null,dark) {onPage(ProposalPage.Paused)}
                }
            }
        }
    }
}
@Composable private fun WatchRow(title:String,state:ProposalState,meta:String?,dark:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,color=MaterialTheme.colorScheme.surfaceContainerLowest) {
        ListItem(
            headlineContent={Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)},
            supportingContent={Row(Modifier.padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Status(state,dark,compact=true)
                if(meta!=null) Text(meta,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }},
            trailingContent={Symbol(R.drawable.symbol_chevron_right,description="Ouvrir le suivi")},
            modifier=Modifier.padding(horizontal=4.dp,vertical=12.dp),
            colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLowest),
        )
    }
}
@Composable private fun Placeholder(modifier:Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh,RoundedCornerShape(4.dp)))
}

@Composable private fun DetailContent(page:ProposalPage,dark:Boolean,modifier:Modifier,onPage:(ProposalPage)->Unit) {
    val preparing=page==ProposalPage.Preparing
    val paused=page==ProposalPage.Paused
    val state=if(preparing) ProposalState.Preparing else if(paused) ProposalState.Paused else ProposalState.Active
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(top=12.dp,bottom=24.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(preparing) "Offres d’emploi Android à Montpellier" else if(paused) "Sortie du prochain iPhone" else "Promotions PlayStation 5",
            style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Status(state,dark)
                if(!preparing) {
                    val large=LocalDensity.current.fontScale>1.15f
                    if(large) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        DateColumn("Dernier contrôle","2 nov. · 09:41")
                        DateColumn("Prochain contrôle",if(paused) "À la reprise" else "2 nov. · 10:41")
                    } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) {DateColumn("Dernier contrôle","2 nov. · 09:41")}
                        Box(Modifier.weight(1f)) {DateColumn("Prochain contrôle",if(paused) "À la reprise" else "2 nov. · 10:41")}
                    }
                }
                if(paused) Button(onClick={onPage(ProposalPage.Active)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)) {
                    Text("Reprendre le suivi")
                } else FilledTonalButton(onClick={onPage(ProposalPage.Paused)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.filledTonalButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                    Symbol(if(preparing) R.drawable.symbol_close else R.drawable.symbol_pause)
                    Spacer(Modifier.width(8.dp));Text(if(preparing) "Annuler la préparation" else "Mettre en pause")
                }
            }
        }
        if(!preparing) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Résultats",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            Text("Aucun résultat pour le moment.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("Ce que tu suis") {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(preparing) "Offres de développeur Android à Montpellier, en France." else if(paused) "Annonce officielle du prochain iPhone." else "Promotions sur la PlayStation 5 disponibles en France.",style=MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                if(LocalDensity.current.fontScale>1.15f) Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("Notifications",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if(paused) "Au premier résultat" else "À chaque nouveau résultat",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Notifications",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if(paused) "Au premier résultat" else "À chaque nouveau résultat",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                }
            }
        }
        Section("Sources suivies") {
            if(preparing) Text("Recherche des sources en cours.",Modifier.padding(16.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else ListItem(
                headlineContent={Text(if(paused) "Apple Newsroom" else "PlayStation Direct",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)},
                supportingContent={Text(if(paused) "apple.com" else "direct.playstation.com",style=MaterialTheme.typography.bodySmall)},
                trailingContent={IconButton(onClick={}) {Symbol(R.drawable.symbol_open_in_new,description="Ouvrir le site")}},
                colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier=Modifier.padding(vertical=2.dp),
            )
        }
    }
}
@Composable private fun DateColumn(label:String,value:String) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun Section(title:String,content:@Composable ()->Unit) {
    Column(Modifier.then(if(title=="Sources suivies") Modifier.testTag("proposal_sources") else Modifier),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest,content=content)
    }
}
