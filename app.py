from flask import Flask, render_template, request, redirect, send_file
from io import BytesIO

app = Flask(__name__)

notes = []

@app.route("/")
def index():
    return render_template("index.html", notes=notes)

@app.route("/add", methods=["POST"])
def add():
    note = request.form.get("note")
    if note:
        notes.append(note)
    return redirect("/")

@app.route("/clear")
def clear():
    notes.clear()
    return redirect("/")

@app.route("/download")
def download():
    text = "\n".join(notes)
    file = BytesIO()
    file.write(text.encode())
    file.seek(0)
    return send_file(file,
                     as_attachment=True,
                     download_name="catatan.txt",
                     mimetype="text/plain")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
