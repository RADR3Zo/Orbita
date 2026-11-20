// Ayudante de Órbita: lee de los controles multimedia de Windows (SMTC) la canción, el estado y la posición
// exacta de Spotify, y lo escribe como una línea JSON cada vez que cambia. Lo arranca el mod de Minecraft
// mientras las letras están activadas y se cierra solo cuando el juego termina (PID del padre como argumento).
//
// Compilar (lo hace build.ps1):
//   csc /target:winexe /out:orbita-media.exe /r:Windows.Media.winmd /r:Windows.Foundation.winmd
//       /r:System.Runtime.dll OrbitaMedia.cs
using System;
using System.Diagnostics;
using System.Text;
using System.Threading;
using Windows.Foundation;
using Windows.Media.Control;

internal static class OrbitaMedia
{
    private static void Main(string[] args)
    {
        int parent = args.Length > 0 ? int.Parse(args[0]) : -1;
        var stdout = Console.OpenStandardOutput();
        GlobalSystemMediaTransportControlsSessionManager manager = null;
        string last = null;
        long lastBeat = 0;
        while (true)
        {
            if (parent > 0 && !Alive(parent))
            {
                return;
            }
            string line;
            try
            {
                if (manager == null)
                {
                    manager = Await(GlobalSystemMediaTransportControlsSessionManager.RequestAsync());
                }
                line = Describe(manager);
            }
            catch (Exception e)
            {
                manager = null;
                line = "{\"error\":" + Json(e.GetType().Name + ": " + e.Message) + "}";
            }
            long now = Environment.TickCount;
            // Se escribe al cambiar algo, y cada 10 s como latido (si el juego cerró la tubería, termina).
            if (line != last || now - lastBeat > 10000)
            {
                try
                {
                    byte[] bytes = Encoding.UTF8.GetBytes(line + "\n");
                    stdout.Write(bytes, 0, bytes.Length);
                    stdout.Flush();
                }
                catch (Exception)
                {
                    return;
                }
                last = line;
                lastBeat = now;
            }
            Thread.Sleep(400);
        }
    }

    private static string Describe(GlobalSystemMediaTransportControlsSessionManager manager)
    {
        GlobalSystemMediaTransportControlsSession spotify = null;
        foreach (var s in manager.GetSessions())
        {
            if (s.SourceAppUserModelId.IndexOf("spotify", StringComparison.OrdinalIgnoreCase) >= 0)
            {
                spotify = s;
                break;
            }
        }
        if (spotify == null)
        {
            return "{\"running\":false}";
        }
        var props = Await(spotify.TryGetMediaPropertiesAsync());
        var timeline = spotify.GetTimelineProperties();
        var playback = spotify.GetPlaybackInfo();
        bool playing = playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing;
        long pos = (long)timeline.Position.TotalMilliseconds;
        long dur = (long)timeline.EndTime.TotalMilliseconds;
        // Momento (ms Unix) en que Windows midió esa posición: el juego extrapola desde ahí.
        long at = timeline.LastUpdatedTime.ToUnixTimeMilliseconds();
        return "{\"running\":true,\"playing\":" + (playing ? "true" : "false")
            + ",\"title\":" + Json(props.Title) + ",\"artist\":" + Json(props.Artist)
            + ",\"album\":" + Json(props.AlbumTitle)
            + ",\"pos\":" + pos + ",\"dur\":" + dur + ",\"at\":" + at + "}";
    }

    /** Espera una operación asíncrona de Windows sin depender de las extensiones de .NET para WinRT. */
    private static T Await<T>(IAsyncOperation<T> op)
    {
        int waited = 0;
        while (op.Status == AsyncStatus.Started)
        {
            Thread.Sleep(5);
            waited += 5;
            if (waited > 5000)
            {
                op.Cancel();
                throw new TimeoutException("Windows no respondió");
            }
        }
        if (op.Status == AsyncStatus.Error)
        {
            throw op.ErrorCode;
        }
        return op.GetResults();
    }

    private static bool Alive(int pid)
    {
        try
        {
            return !Process.GetProcessById(pid).HasExited;
        }
        catch (Exception)
        {
            return false;
        }
    }

    private static string Json(string s)
    {
        if (s == null)
        {
            return "\"\"";
        }
        var sb = new StringBuilder("\"");
        foreach (char c in s)
        {
            switch (c)
            {
                case '"': sb.Append("\\\""); break;
                case '\\': sb.Append("\\\\"); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': break;
                case '\t': sb.Append(' '); break;
                default:
                    if (c < 0x20) { sb.Append(' '); } else { sb.Append(c); }
                    break;
            }
        }
        return sb.Append('"').ToString();
    }
}
