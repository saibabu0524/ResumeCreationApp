BASE_LATEX_TEMPLATE = r'''\documentclass{article}
\usepackage{fontawesome5}

% Packages:
\usepackage[
    ignoreheadfoot,
    top=2 cm,
    bottom=2 cm,
    left=2 cm,
    right=2 cm,
    footskip=1.0 cm,
]{geometry}
\usepackage{titlesec, tabularx, array, xcolor, enumitem, fontawesome5, amsmath, hyperref, eso-pic, calc, bookmark, lastpage, changepage, paracol, ifthen, needspace, iftex}
\definecolor{primaryColor}{RGB}{0, 0, 0}

\ifPDFTeX
    \input{glyphtounicode}
    \pdfgentounicode=1
    \usepackage[T1]{fontenc}
    \usepackage[utf8]{inputenc}
    \usepackage{lmodern}
\fi

\usepackage{charter}

\raggedright
\pagestyle{empty}
\setcounter{secnumdepth}{0}
\setlength{\parindent}{0pt}
\setlength{\topskip}{0pt}
\setlength{\columnsep}{0.15cm}
\pagenumbering{gobble}

\titleformat{\section}{\needspace{4\baselineskip}\bfseries\large}{}{0pt}{}[\vspace{1pt}\titlerule]
\titlespacing{\section}{-1pt}{0.3 cm}{0.2 cm}

% Improved list environment
\newenvironment{highlights}{
    \begin{itemize}[topsep=0.10 cm, parsep=0.10 cm, partopsep=0pt, itemsep=0pt, leftmargin=15pt]
}{
    \end{itemize}
}

\begin{document}

%----------HEADING----------
\begin{center}
{\fontsize{25 pt}{25 pt}\selectfont {{NAME}}}\vspace{5pt}
\end{center}

\begin{center}
    {{CONTACT_INFO}}
\end{center}

%-----------SUMMARY-----------
\section*{Professional Summary}
\noindent
{{SUMMARY}}

%-----------TECHNICAL SKILLS-----------
\section{Technical Skills}
\begin{itemize}[leftmargin=15pt, itemsep=2pt]
    {{SKILLS}}
\end{itemize}

%-----------EXPERIENCE-----------
\section{Experience}
{{EXPERIENCE}}

%-----------PROJECTS-----------
\section{Projects}
{{PROJECTS}}

%-----------ACHIEVEMENTS-----------
\section{Achievements \& Certification}
\begin{highlights}
{{ACHIEVEMENTS}}
\end{highlights}

%-----------EDUCATION-----------
\section{Education}
{{EDUCATION}}

\end{document}
'''

# --- Formatting Guidance ---
# Contact info should use \faMapMarker*, \faPhone, \faEnvelope, \faLinkedin, \faGithub icons
# separated by \hspace{5pt}|\hspace{5pt}
#
# Experience entries follow this pattern:
#   \textbf{Title, Company} \hfill (Start -- End)\\[2mm]
#   Brief description paragraph.
#   \begin{itemize}[leftmargin=15pt, itemsep=2pt]
#       \item ...
#   \end{itemize}
#
# Project entries follow this pattern:
#   \textbf{Project Name} \hfill \textit{Date | Tech Stack}\\
#   Brief description.
#   \begin{highlights}
#       \item \textbf{Key aspect:} Detail
#   \end{highlights}
#
# Skill items:   \item \textbf{Category:} value1, value2, ...
# Education:     \textbf{Degree} \hfill \textit{Years} \\ Institution
# Achievement items go directly as \item text inside the highlights env


STAGE_A_PROMPT = r"""
You are an expert resume formatter. Your task is to take raw text extracted from a user's resume and format it into the provided LaTeX template.

STRICT RULES:
1. Preserve EVERY piece of information — do NOT paraphrase, summarize, or omit anything.
2. Fill in these placeholders: {{NAME}}, {{CONTACT_INFO}}, {{SUMMARY}}, {{SKILLS}}, {{EXPERIENCE}}, {{PROJECTS}}, {{ACHIEVEMENTS}}, {{EDUCATION}}.
3. If a section (e.g., Achievements, Projects) is absent from the raw text, remove that entire section block from the LaTeX.
4. Escape ALL LaTeX special characters: & → \&, % → \%, $ → \$, # → \#, _ → \_, {{ → \{{, }} → \}}, ~ → \textasciitilde, ^ → \textasciicircum, \ → \textbackslash
5. Contact info: use FontAwesome5 icons (\faMapMarker*, \faPhone~, \faEnvelope~, \faLinkedin~, \faGithub~) for each contact field, separated by \hspace{5pt}|\hspace{5pt}.
6. Skills: each category on its own \item \textbf{Category:} values line.
7. Experience: use \textbf{Title, Company} \hfill (dates)\\[2mm] + description paragraph + \begin{itemize}...\end{itemize}.
8. Projects: use \textbf{Name} \hfill \textit{dates | tech}\\  + description + \begin{highlights}...\end{highlights}.
9. Education: \textbf{Degree} \hfill \textit{Years} \\ Institution
10. Output ONLY valid LaTeX wrapped in <latex>...</latex> tags. No preamble, no explanation, nothing else.

LaTeX Template:
```latex
{latex_template}
```

Raw Resume Text:
```
{resume_text}
```
"""

STAGE_B_PROMPT = r"""
You are an expert technical recruiter and resume writer.
Your task is to tailor a candidate's LaTeX resume to perfectly match a provided Job Description.

INSTRUCTIONS:
1. Rewrite the Professional Summary to directly target this specific role and company.
2. Mirror keywords, terminology, and required skills from the Job Description in bullet points.
3. Reorder Technical Skills to surface the most relevant technologies first.
4. Elevate bullet points that align with the job's responsibilities; de-emphasize unrelated ones.
5. Do NOT fabricate any experience, skills, certifications, or projects the candidate doesn't have.
6. Do NOT remove any experience or projects — only reword and re-prioritize.
7. Keep ALL LaTeX syntax valid. Escape special characters properly: & → \&, % → \%, $ → \$, # → \#, _ → \_, ~ → \textasciitilde, ^ → \textasciicircum
8. Preserve the template structure exactly (all environments, commands, section order).
9. Output ONLY valid LaTeX wrapped in <latex>...</latex> tags. No preamble, no explanation, nothing else.

Job Description:
```
{job_description}
```

Original LaTeX Resume:
```latex
{latex_resume}
```
"""