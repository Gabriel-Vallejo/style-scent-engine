import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View, Button, Alert, TextInput, ScrollView, ActivityIndicator, TouchableOpacity } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { useState, useEffect } from 'react';

const API_URL = 'http://192.168.1.44:8080/api/prendas';

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('armario');
  const [loading, setLoading] = useState(true);

  // Estados del Armario
  const [misPrendas, setMisPrendas] = useState([]);
  const [prendasSeleccionadas, setPrendasSeleccionadas] = useState([]);

  // Estados del Registro
  const [categorias, setCategorias] = useState([]);
  const [colores, setColores] = useState([]);
  const [estilos, setEstilos] = useState([]);
  const [nombre, setNombre] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [colorId, setColorId] = useState('');
  const [estiloId, setEstiloId] = useState('');

  useEffect(() => {
    cargarDatos();
  }, []);

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const [resCat, resCol, resEst, resPrendas] = await Promise.all([
        fetch(`${API_URL}/categorias`),
        fetch(`${API_URL}/colores`),
        fetch(`${API_URL}/estilos`),
        fetch(API_URL)
      ]);

      const dataCat = await resCat.json();
      const dataCol = await resCol.json();
      const dataEst = await resEst.json();
      const dataPrendas = await resPrendas.json();

      setCategorias(dataCat);
      setColores(dataCol);
      setEstilos(dataEst);
      setMisPrendas(dataPrendas);

      if(dataCat.length > 0) setCategoriaId(dataCat[0].idCategoria);
      if(dataCol.length > 0) setColorId(dataCol[0].idColor);
      if(dataEst.length > 0) setEstiloId(dataEst[0].idEstilo);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const registrarPrenda = async () => {
    if (!nombre.trim()) return Alert.alert("Aviso", "Escribe un nombre.");
    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nombre, categoriaId, colorId, estilosIds: [estiloId] }),
      });
      if (response.ok) {
        Alert.alert("¡Éxito!", "Prenda guardada.");
        setNombre('');
        cargarDatos();
        setVistaActiva('armario');
      }
    } catch (error) {
      Alert.alert("Error", "No se pudo guardar.");
    }
  };

  const togglePrenda = (idPrenda) => {
    if (prendasSeleccionadas.includes(idPrenda)) {
      setPrendasSeleccionadas(prendasSeleccionadas.filter(id => id !== idPrenda));
    } else {
      setPrendasSeleccionadas([...prendasSeleccionadas, idPrenda]);
    }
  };

  const solicitarRecomendacion = async () => {
    if (prendasSeleccionadas.length === 0) {
      Alert.alert("Aviso", "Selecciona al menos una prenda para tu outfit.");
      return;
    }

    Alert.alert("Motor Scent", "Calculando sinergias...");

    try {
      const response = await fetch(`${API_URL}/match`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(prendasSeleccionadas)
      });

      const data = await response.json();

      if (response.ok) {
        Alert.alert("✨ Tu Perfume Ideal", `Te recomiendo:\n\n${data.perfumeRecomendado}`);
      } else {
        Alert.alert("Error del Motor", data.error || "No se pudo calcular.");
      }
    } catch (error) {
      Alert.alert("Error de red", "No se pudo conectar con el motor de Spring Boot.");
    }
  };

  if (loading) {
    return (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#000" />
        </View>
    );
  }

  return (
      <View style={{ flex: 1, backgroundColor: '#f5f5f5' }}>
        <View style={styles.navBar}>
          <TouchableOpacity style={[styles.navTab, vistaActiva === 'armario' && styles.navTabActive]} onPress={() => setVistaActiva('armario')}>
            <Text style={[styles.navText, vistaActiva === 'armario' && styles.navTextActive]}>MI ARMARIO</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.navTab, vistaActiva === 'registro' && styles.navTabActive]} onPress={() => setVistaActiva('registro')}>
            <Text style={[styles.navText, vistaActiva === 'registro' && styles.navTextActive]}>AÑADIR PRENDA</Text>
          </TouchableOpacity>
        </View>

        {vistaActiva === 'armario' && (
            <ScrollView contentContainerStyle={styles.container}>
              <Text style={styles.title}>Selecciona tu Outfit</Text>
              {misPrendas.map(prenda => {
                const id = prenda.idPrenda || prenda.id;
                const isSelected = prendasSeleccionadas.includes(id);
                return (
                    <TouchableOpacity
                        key={id}
                        style={[styles.prendaCard, isSelected && styles.prendaCardSelected]}
                        onPress={() => togglePrenda(id)}
                    >
                      <Text style={[styles.prendaName, isSelected && styles.prendaTextSelected]}>{prenda.nombre}</Text>
                      <Text style={styles.prendaSub}>Color ID: {prenda.color?.idColor} | Categ: {prenda.categoria?.idCategoria}</Text>
                    </TouchableOpacity>
                )
              })}

              <TouchableOpacity style={styles.btnRecomendar} onPress={solicitarRecomendacion}>
                <Text style={styles.btnRecomendarText}>RECOMENDAR PERFUME</Text>
              </TouchableOpacity>
            </ScrollView>
        )}

        {vistaActiva === 'registro' && (
            <ScrollView contentContainerStyle={styles.container}>
              <Text style={styles.title}>Nueva Prenda</Text>
              <Text style={styles.label}>Nombre</Text>
              <TextInput style={styles.input} value={nombre} onChangeText={setNombre} />

              <Text style={styles.label}>Categoría</Text>
              <View style={styles.pickerContainer}>
                <Picker selectedValue={categoriaId} onValueChange={setCategoriaId}>
                  {categorias.map(cat => <Picker.Item key={cat.idCategoria} label={cat.nombre} value={cat.idCategoria} />)}
                </Picker>
              </View>

              <Text style={styles.label}>Color</Text>
              <View style={styles.pickerContainer}>
                <Picker selectedValue={colorId} onValueChange={setColorId}>
                  {colores.map(col => <Picker.Item key={col.idColor} label={col.nombre} value={col.idColor} />)}
                </Picker>
              </View>

              <Text style={styles.label}>Estilo</Text>
              <View style={styles.pickerContainer}>
                <Picker selectedValue={estiloId} onValueChange={setEstiloId}>
                  {estilos.map(est => <Picker.Item key={est.idEstilo} label={est.nombre} value={est.idEstilo} />)}
                </Picker>
              </View>

              <TouchableOpacity style={[styles.btnRecomendar, {marginTop: 30, backgroundColor: '#333'}]} onPress={registrarPrenda}>
                <Text style={styles.btnRecomendarText}>GUARDAR</Text>
              </TouchableOpacity>
            </ScrollView>
        )}
        <StatusBar style="auto" />
      </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  container: { padding: 20 },
  navBar: { flexDirection: 'row', paddingTop: 50, backgroundColor: '#fff', borderBottomWidth: 1, borderColor: '#ddd' },
  navTab: { flex: 1, paddingVertical: 15, alignItems: 'center' },
  navTabActive: { borderBottomWidth: 3, borderBottomColor: '#000' },
  navText: { fontWeight: '600', color: '#888' },
  navTextActive: { color: '#000' },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 20, textAlign: 'center', marginTop: 10 },
  label: { fontSize: 16, fontWeight: '600', marginTop: 10, marginBottom: 5 },
  input: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 16 },
  pickerContainer: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#ddd', borderRadius: 8, overflow: 'hidden' },
  prendaCard: { backgroundColor: '#fff', padding: 15, borderRadius: 10, marginBottom: 10, borderWidth: 1, borderColor: '#ddd' },
  prendaCardSelected: { backgroundColor: '#000', borderColor: '#000' },
  prendaName: { fontSize: 16, fontWeight: 'bold' },
  prendaTextSelected: { color: '#fff' },
  prendaSub: { fontSize: 12, color: '#666', marginTop: 4 },
  btnRecomendar: { backgroundColor: '#0056b3', padding: 15, borderRadius: 10, alignItems: 'center', marginTop: 20 },
  btnRecomendarText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});