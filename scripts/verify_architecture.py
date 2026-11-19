"""Fast architecture guardrails; complements, and does not replace, the Android build."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main'
PACKAGE = MAIN / 'java/com/unpad/padukampus'
required = {
    'PaduKampusApp.kt', 'di/AppContainer.kt', 'core/common/AppResult.kt',
    'core/util/DateTimeUtils.kt', 'core/util/NetworkMonitor.kt', 'core/constants/AppConstants.kt',
    'data/local/db/PaduKampusDatabase.kt', 'data/local/relation/JadwalDetail.kt', 'data/local/relation/TugasDetail.kt',
    'data/remote/api/OpenMeteoApi.kt', 'data/remote/dto/WeatherDto.kt', 'data/remote/sync/CloudSyncManager.kt',
    'worker/TaskReminderWorker.kt', 'worker/TaskReminderScheduler.kt', 'worker/NotificationHelper.kt', 'ui/main/MainActivity.kt'
}
for entity in ['Gedung','Ruangan','MataKuliah','Jadwal','PerubahanJadwal','Tugas','CacheCuaca']:
    required.add(f'data/local/entity/{entity}Entity.kt')
for domain in ['Kampus','Jadwal','Tugas','Cuaca']:
    required.add(f'data/local/dao/{domain}Dao.kt'); required.add(f'data/repository/{domain}Repository.kt')
for feature, names in {
    'home':['HomeFragment','HomeViewModel','HomeUiState'],
    'schedule':['ScheduleFragment','ScheduleViewModel','ScheduleUiState','ScheduleAdapter'],
    'tasks':['TaskListFragment','TaskFormFragment','TaskViewModel','TaskUiState','TaskAdapter'],
    'map':['MapFragment','MapViewModel','MapUiState'],
    'profile':['ProfileFragment','ProfileViewModel','ProfileUiState']
}.items():
    required.update(f'ui/{feature}/{name}.kt' for name in names)
assert all((PACKAGE / name).is_file() for name in required), 'Required architecture files are missing'
assert len(list((PACKAGE/'data/local/entity').glob('*.kt'))) == 7
for path in MAIN.rglob('*.kt'):
    content = path.read_text(encoding='utf-8')
    assert 'id.ac.unpad.padukampus' not in content, path
    expected = 'package ' + '.'.join(path.relative_to(MAIN/'java').parts[:-1])
    assert content.startswith(expected + '\n'), path
for path in (PACKAGE/'ui').rglob('*.kt'):
    content = path.read_text(encoding='utf-8')
    assert not re.search(r'getSharedPreferences|data\.local\.(dao|db)|import retrofit2|import androidx\.work', content), path
    if path.name.endswith('Fragment.kt'):
        assert 'repeatOnLifecycle' in content and 'onDestroyView' in content and 'Binding' in content, path
for path in MAIN.rglob('*.xml'): ET.parse(path)
android = '{http://schemas.android.com/apk/res/android}'
graph = ET.parse(MAIN/'res/navigation/nav_graph.xml').getroot()
destinations = graph.findall('fragment')
ids = {node.attrib[android+'id'].split('/')[-1] for node in destinations}
assert len(destinations) == 6
menu = ET.parse(MAIN/'res/menu/bottom_navigation.xml').getroot()
assert len(menu) == 5 and all(node.attrib[android+'id'].split('/')[-1] in ids for node in menu)
for node in destinations:
    assert (MAIN/'java'/(node.attrib[android+'name'].replace('.', '/')+'.kt')).is_file()
task_form = (PACKAGE/'ui/tasks/TaskFormFragment.kt').read_text()
task_list = (PACKAGE/'ui/tasks/TaskListFragment.kt').read_text()
assert 'by navArgs()' in task_form and 'TaskListFragmentDirections' in task_list
for path in (PACKAGE/'ui').rglob('*.kt'):
    assert not re.search(r'bundleOf\(|arguments\??\.get|putString\("taskId"', path.read_text()), path
build = (ROOT/'app/build.gradle.kts').read_text()
assert 'viewBinding = true' in build and 'androidx.navigation.safeargs.kotlin' in build
for key,value in [('compileSdk',35),('targetSdk',35),('minSdk',26)]:
    assert re.search(rf'{key}\s*=\s*{value}\b', build)
plugins = (ROOT/'build.gradle.kts').read_text()
for plugin, version in {
    'com.android.application':'8.9.2', 'org.jetbrains.kotlin.android':'2.1.20',
    'com.google.devtools.ksp':'2.1.20-1.0.32', 'androidx.navigation.safeargs.kotlin':'2.8.9'
}.items():
    assert f'id("{plugin}") version "{version}"' in plugins
for dependency in ['androidx.room:room-runtime:2.7.1', 'androidx.room:room-compiler:2.7.1',
    'com.squareup.retrofit2:retrofit:2.11.0', 'com.squareup.retrofit2:converter-gson:2.11.0',
    'androidx.navigation:navigation-fragment-ktx:2.8.9', 'androidx.work:work-runtime-ktx:2.10.0']:
    assert dependency in build
wrapper = (ROOT/'gradle/wrapper/gradle-wrapper.properties').read_text()
assert 'gradle-8.11.1-bin.zip' in wrapper and 'distributionSha256Sum=' in wrapper
assert 'fallbackToDestructiveMigration' not in (PACKAGE/'di/AppContainer.kt').read_text()
print(f'PASS: {len(required)} required files, seven entities, MVVM boundaries, six destinations, Safe Args, XML and build baseline.')
